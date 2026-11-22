package net.blazarforce.folio.comic

import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Subset of the ComicRack ComicInfo.xml schema that matters to a reader. */
data class ComicInfo(
    val title: String? = null,
    val series: String? = null,
    val number: String? = null,
    val volume: String? = null,
    val summary: String? = null,
    val writer: String? = null,
    val pageCount: Int? = null,
    /** True when Manga = YesAndRightToLeft (or Yes): read right-to-left. */
    val rightToLeft: Boolean = false,
    val frontCoverIndex: Int? = null,
    val doublePages: Set<Int> = emptySet(),
)

object ComicInfoParser {
    fun parse(input: InputStream): ComicInfo? = try {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { isExpandEntityReferences = false }
        }
        val doc = factory.newDocumentBuilder().parse(input)
        val root = doc.documentElement
        fun text(tag: String): String? {
            val nodes = root.getElementsByTagName(tag)
            for (k in 0 until nodes.length) {
                val n = nodes.item(k)
                if (n.parentNode == root) return n.textContent?.trim()?.takeIf { it.isNotEmpty() }
            }
            return null
        }
        var cover: Int? = null
        val doubles = mutableSetOf<Int>()
        val pages = root.getElementsByTagName("Page")
        for (k in 0 until pages.length) {
            val p = pages.item(k) as? Element ?: continue
            val idx = p.getAttribute("Image").toIntOrNull() ?: continue
            val type = p.getAttribute("Type")
            if (cover == null && type.equals("FrontCover", ignoreCase = true)) cover = idx
            if (p.getAttribute("DoublePage").equals("true", ignoreCase = true)) doubles += idx
        }
        val manga = text("Manga")
        ComicInfo(
            title = text("Title"),
            series = text("Series"),
            number = text("Number"),
            volume = text("Volume"),
            summary = text("Summary"),
            writer = text("Writer"),
            pageCount = text("PageCount")?.toIntOrNull(),
            rightToLeft = manga.equals("YesAndRightToLeft", true) || manga.equals("Yes", true),
            frontCoverIndex = cover,
            doublePages = doubles,
        )
    } catch (e: Exception) {
        null
    }
}
