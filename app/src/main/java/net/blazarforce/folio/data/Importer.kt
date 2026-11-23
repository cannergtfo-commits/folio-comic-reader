package net.blazarforce.folio.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.blazarforce.folio.comic.CbzArchive
import net.blazarforce.folio.comic.CbzPageSource
import net.blazarforce.folio.comic.FolderPageSource
import net.blazarforce.folio.comic.NameHeuristics
import net.blazarforce.folio.comic.NaturalOrder
import net.blazarforce.folio.comic.PageSource
import net.blazarforce.folio.comic.PdfPageSource
import java.io.File
import kotlin.math.roundToInt

/** Brings files into the library: CBZ / PDF are copied privately; image folders keep a persisted tree grant. */
class Importer(private val context: Context, private val store: LibraryStore) {
    private val booksDir = File(context.filesDir, "books").apply { mkdirs() }
    private val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

    private data class Meta(val name: String, val size: Long)

    private fun meta(uri: Uri): Meta {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "comic"
        var size = -1L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        c.getString(0)?.let { name = it }
                        if (!c.isNull(1)) size = c.getLong(1)
                    }
                }
        }
        return Meta(name, size)
    }

    private fun idFor(vararg parts: String): String = bookIdFor(*parts)

    /** Imports one CBZ/PDF document. Returns the (possibly pre-existing) book, or null if unsupported. */
    suspend fun importDocument(uri: Uri): Book? = withContext(io) {
        val m = meta(uri)
        val id = idFor(m.name, m.size.toString())
        store.get(id)?.let { return@withContext it }

        val tmp = File(booksDir, "$id.part")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { input.copyTo(it, 256 * 1024) }
        } ?: return@withContext null
        adopt(tmp, id, m.name)
    }

    /** Partial download target for [importDownloaded]. */
    fun downloadTarget(id: String): File = File(booksDir, "$id.download")

    /** Adds a finished download (already in app storage) to the shelf under the same id a manual import would get. */
    suspend fun importDownloaded(file: File, name: String): Book? = withContext(io) {
        val id = idFor(name, file.length().toString())
        store.get(id)?.let { file.delete(); return@withContext it }
        adopt(file, id, name)
    }

    private fun adopt(tmp: File, id: String, name: String): Book? {
        val head = ByteArray(4).also { b -> tmp.inputStream().use { it.read(b) } }
        val kind = when {
            head[0] == '%'.code.toByte() && head[1] == 'P'.code.toByte() -> BookKind.PDF
            head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() -> BookKind.CBZ
            else -> null
        }
        if (kind == null) { tmp.delete(); return null }
        val dest = File(booksDir, "$id.${if (kind == BookKind.PDF) "pdf" else "cbz"}")
        tmp.renameTo(dest)
        return runCatching { buildBook(id, kind, dest, name) }.getOrElse { dest.delete(); null }
            ?.also { store.add(it) }
    }

    private fun buildBook(id: String, kind: BookKind, file: File, name: String): Book? {
        val guess = NameHeuristics.guess(name)
        return when (kind) {
            BookKind.CBZ -> CbzPageSource(file).use { src ->
                val a = src.archive
                if (a.pageCount == 0) return null
                val info = a.info
                val series = info?.series ?: guess.series
                val number = info?.number ?: guess.number
                Book(
                    id = id, kind = kind, location = file.absolutePath,
                    title = info?.title ?: guess.title,
                    series = series, number = number,
                    numberLabel = if (info?.number != null) (guess.label ?: "No.") else guess.label,
                    pageCount = a.pageCount,
                    coverPath = writeCover(id, src, a.coverIndex),
                    rightToLeft = info?.rightToLeft == true,
                    doublePages = info?.doublePages ?: emptySet(),
                    coverIndex = a.coverIndex,
                    sourceName = name, sizeBytes = file.length(),
                )
            }
            BookKind.PDF -> PdfPageSource(file).use { src ->
                if (src.pageCount == 0) return null
                Book(
                    id = id, kind = kind, location = file.absolutePath, title = guess.title,
                    series = guess.series, number = guess.number, numberLabel = guess.label,
                    pageCount = src.pageCount, coverPath = writeCover(id, src, 0),
                    sourceName = name, sizeBytes = file.length(),
                )
            }
            BookKind.FOLDER -> null
        }
    }

    private fun writeCover(id: String, src: PageSource, index: Int): String? {
        val bmp = src.decode(index, 640, 960) ?: return null
        val w = 480
        val scaled = if (bmp.width > w) Bitmap.createScaledBitmap(bmp, w, (bmp.height * w / bmp.width.toFloat()).roundToInt(), true) else bmp
        val out = File(coversDir, "$id.jpg")
        out.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return out.absolutePath
    }

    private data class Child(val docId: String, val name: String, val mime: String)

    private fun children(tree: Uri, docId: String): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val out = mutableListOf<Child>()
        context.contentResolver.query(
            uri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null, null, null,
        )?.use { c -> while (c.moveToNext()) out += Child(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: "") }
        return out.sortedWith { a, b -> NaturalOrder.compare(a.name, b.name) }
    }

    /** Imports every CBZ/PDF in a picked folder (and one level of sub-folders); image folders become books. */
    suspend fun importTree(tree: Uri): List<Book> = withContext(io) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val rootName = DocumentsContract.buildDocumentUriUsingTree(tree, rootId).let { meta(it).name }
        val results = mutableListOf<Book>()
        suspend fun scan(docId: String, name: String, parent: String?, depth: Int) {
            val kids = children(tree, docId)
            kids.filter { isComicFile(it) }.forEach { k ->
                importDocument(DocumentsContract.buildDocumentUriUsingTree(tree, k.docId))?.let { results += it }
            }
            val images = kids.filter { CbzArchive.isImageName(it.name) }
            if (images.isNotEmpty()) folderBook(tree, docId, name, parent, images)?.let { results += it }
            if (depth < 2) kids.filter { it.mime == DocumentsContract.Document.MIME_TYPE_DIR }
                .forEach { scan(it.docId, it.name, name, depth + 1) }
        }
        scan(rootId, rootName, null, 0)
        results
    }

    private fun isComicFile(c: Child): Boolean {
        val n = c.name.lowercase()
        return n.endsWith(".cbz") || n.endsWith(".pdf") || c.mime == "application/pdf" ||
            c.mime == "application/vnd.comicbook+zip"
    }

    private fun folderBook(tree: Uri, docId: String, name: String, parent: String?, images: List<Child>): Book? {
        val id = idFor(tree.toString(), docId)
        store.get(id)?.let { return it }
        val uris = images.map { DocumentsContract.buildDocumentUriUsingTree(tree, it.docId) }
        val guess = NameHeuristics.guess(name)
        val coverIdx = images.indexOfFirst { it.name.lowercase().startsWith("cover") }.takeIf { it >= 0 } ?: 0
        val src = FolderPageSource(context, uris)
        val book = Book(
            id = id, kind = BookKind.FOLDER, location = "$tree#$docId",
            title = guess.title,
            series = guess.series ?: parent?.let { NameHeuristics.clean(it) }?.takeIf { guess.number != null },
            number = guess.number, numberLabel = guess.label,
            pageCount = uris.size, coverPath = writeCover(id, src, coverIdx), coverIndex = coverIdx,
            sourceName = name,
        )
        store.add(book)
        return book
    }

    companion object {
        /** Overridable for Robolectric tests, whose native graphics can't encode off the test thread. */
        @Volatile var io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO

        /** Opens the right page source for a library book. Caller must close it. */
        fun openSource(context: Context, book: Book): PageSource = when (book.kind) {
            BookKind.CBZ -> CbzPageSource(File(book.location))
            BookKind.PDF -> PdfPageSource(File(book.location))
            BookKind.FOLDER -> {
                val tree = Uri.parse(book.location.substringBefore('#'))
                val docId = book.location.substringAfter('#')
                val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
                val pages = mutableListOf<Pair<String, Uri>>()
                context.contentResolver.query(
                    uri,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        val n = c.getString(1) ?: continue
                        if (CbzArchive.isImageName(n)) pages += n to DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                    }
                }
                FolderPageSource(context, pages.sortedWith { a, b -> NaturalOrder.compare(a.first, b.first) }.map { it.second })
            }
        }
    }
}
