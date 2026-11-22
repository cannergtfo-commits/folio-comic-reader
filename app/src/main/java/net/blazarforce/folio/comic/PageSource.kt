package net.blazarforce.folio.comic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Random-access page provider. Implementations must be safe to call from several IO threads. */
interface PageSource : Closeable {
    val pageCount: Int

    /** Width / height of a page, or null when unknown. Cheap-ish (header only). */
    fun aspect(index: Int): Float?

    /** Decodes a page so that it fits (at least) the requested box, never larger than needed. */
    fun decode(index: Int, reqWidth: Int, reqHeight: Int): Bitmap?
}

internal fun sampleSizeFor(w: Int, h: Int, reqW: Int, reqH: Int): Int {
    if (w <= 0 || h <= 0) return 1
    // Scale that fits the page inside the requested box.
    val fit = min(reqW.toFloat() / w, reqH.toFloat() / h)
    var sample = 1
    while ((w / (sample * 2)) >= w * fit && (h / (sample * 2)) >= h * fit) sample *= 2
    return sample
}

/** Shared decode for byte-stream sources (CBZ, folders). */
internal abstract class StreamPageSource : PageSource {
    private val aspects = HashMap<Int, Float>()
    protected abstract fun openPage(index: Int): InputStream

    override fun aspect(index: Int): Float? {
        synchronized(aspects) { aspects[index]?.let { return it } }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { openPage(index).use { BitmapFactory.decodeStream(it, null, opts) } }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val a = opts.outWidth.toFloat() / opts.outHeight
        synchronized(aspects) { aspects[index] = a }
        return a
    }

    override fun decode(index: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        if (index !in 0 until pageCount) return null
        val bytes = runCatching { openPage(index).use { it.readBytes() } }.getOrNull() ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth > 0) synchronized(aspects) { aspects[index] = bounds.outWidth.toFloat() / bounds.outHeight }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) }.getOrNull()
    }
}

internal class CbzPageSource(file: File) : StreamPageSource() {
    val archive = CbzArchive(file)
    private val lock = Any()
    override val pageCount: Int get() = archive.pageCount
    override fun openPage(index: Int): InputStream =
        synchronized(lock) { archive.readBytes(index) }.inputStream()
    override fun close() = archive.close()
}

/** A folder of images picked with the Storage Access Framework. */
internal class FolderPageSource(private val context: Context, private val pages: List<Uri>) : StreamPageSource() {
    override val pageCount: Int get() = pages.size
    override fun openPage(index: Int): InputStream =
        context.contentResolver.openInputStream(pages[index]) ?: error("Cannot open ${pages[index]}")
    override fun close() {}
}

internal class PdfPageSource(file: File) : PageSource {
    private val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val sizes = HashMap<Int, Pair<Int, Int>>()
    override val pageCount: Int = renderer.pageCount

    private fun size(index: Int): Pair<Int, Int> = synchronized(renderer) {
        sizes.getOrPut(index) { renderer.openPage(index).use { it.width to it.height } }
    }

    override fun aspect(index: Int): Float? = runCatching {
        val (w, h) = size(index); w.toFloat() / h
    }.getOrNull()

    override fun decode(index: Int, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
        synchronized(renderer) {
            renderer.openPage(index).use { page ->
                sizes[index] = page.width to page.height
                val fit = min(reqWidth.toFloat() / page.width, reqHeight.toFloat() / page.height)
                // Never render absurdly large bitmaps; cap at ~3x the PDF's point size.
                val scale = min(fit, 3f)
                val w = max(1, (page.width * scale).roundToInt())
                val h = max(1, (page.height * scale).roundToInt())
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }
    }.getOrNull()

    override fun close() {
        runCatching { renderer.close() }
        runCatching { pfd.close() }
    }
}
