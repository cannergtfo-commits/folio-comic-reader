package net.blazarforce.folio.comic

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Pure-JVM view of a CBZ: naturally ordered image entries plus optional ComicInfo. */
class CbzArchive(file: File) : Closeable {
    private val zip: ZipFile = try {
        ZipFile(file)
    } catch (e: Exception) {
        ZipFile(file, Charset.forName("CP437"))
    }

    val pages: List<ZipEntry>
    val info: ComicInfo?

    init {
        val all = zip.entries().toList()
        pages = all.filter { !it.isDirectory && isImageName(it.name) }
            .sortedWith { a, b -> NaturalOrder.compare(a.name, b.name) }
        val infoEntry = all.firstOrNull {
            !it.isDirectory && it.name.substringAfterLast('/').equals("ComicInfo.xml", ignoreCase = true)
        }
        info = infoEntry?.let { e -> zip.getInputStream(e).use { ComicInfoParser.parse(it) } }
    }

    val pageCount: Int get() = pages.size

    /** Cover index: ComicInfo FrontCover if valid, else the first page. */
    val coverIndex: Int
        get() = info?.frontCoverIndex?.takeIf { it in pages.indices } ?: 0

    fun open(index: Int): InputStream = zip.getInputStream(pages[index])

    fun readBytes(index: Int): ByteArray = open(index).use { it.readBytes() }

    override fun close() = zip.close()

    companion object {
        private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")

        fun isImageName(path: String): Boolean {
            if (path.contains("__MACOSX/")) return false
            val name = path.substringAfterLast('/')
            if (name.startsWith(".")) return false
            return name.substringAfterLast('.', "").lowercase() in IMAGE_EXT
        }
    }
}
