package net.blazarforce.folio

import net.blazarforce.folio.comic.CbzArchive
import net.blazarforce.folio.comic.ComicInfoParser
import net.blazarforce.folio.comic.NameHeuristics
import net.blazarforce.folio.comic.NaturalOrder
import net.blazarforce.folio.comic.buildSpreads
import net.blazarforce.folio.comic.unitOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Pure-JVM tests for CBZ parsing, natural ordering, ComicInfo and spreads. */
class ComicParsingTest {

    @Test
    fun naturalOrderSortsNumbersByMagnitude() {
        val names = listOf("page10.png", "Page2.png", "page1.png", "page02b.png", "cover.png", "page001.png", "page11.png")
        val sorted = names.sortedWith(NaturalOrder)
        assertEquals(listOf("cover.png", "page001.png", "page1.png", "Page2.png", "page02b.png", "page10.png", "page11.png"), sorted)
        assertTrue(NaturalOrder.compare("ch2/p9.jpg", "ch10/p1.jpg") < 0)
        assertTrue(NaturalOrder.compare("a", "a") == 0)
    }

    @Test
    fun comicInfoParsesMangaDoublePagesAndCover() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <ComicInfo>
              <Title>Night Market</Title><Series>Lanterns</Series><Number>3</Number>
              <PageCount>5</PageCount><Manga>YesAndRightToLeft</Manga>
              <Pages>
                <Page Image="1" Type="FrontCover"/>
                <Page Image="3" DoublePage="True"/>
              </Pages>
            </ComicInfo>
        """.trimIndent()
        val info = ComicInfoParser.parse(xml.byteInputStream())!!
        assertEquals("Night Market", info.title)
        assertEquals("Lanterns", info.series)
        assertEquals("3", info.number)
        assertEquals(5, info.pageCount)
        assertTrue(info.rightToLeft)
        assertEquals(1, info.frontCoverIndex)
        assertEquals(setOf(3), info.doublePages)
    }

    @Test
    fun malformedComicInfoIsIgnored() {
        assertEquals(null, ComicInfoParser.parse("<ComicInfo><Title>oops".byteInputStream()))
    }

    @Test
    fun syntheticCbzFiltersJunkAndSortsNaturally() {
        val f = File.createTempFile("folio", ".cbz")
        ZipOutputStream(f.outputStream()).use { z ->
            for (n in listOf("page10.png", "page2.png", "Page1.png", "__MACOSX/._page1.png", ".DS_Store", "notes.txt", "sub/", "ComicInfo.xml")) {
                z.putNextEntry(ZipEntry(n))
                if (n == "ComicInfo.xml") z.write("<ComicInfo><Series>S</Series><Manga>No</Manga></ComicInfo>".toByteArray())
                else if (!n.endsWith("/")) z.write(byteArrayOf(1, 2, 3))
                z.closeEntry()
            }
        }
        CbzArchive(f).use { a ->
            assertEquals(listOf("Page1.png", "page2.png", "page10.png"), a.pages.map { it.name })
            assertEquals("S", a.info?.series)
            assertFalse(a.info!!.rightToLeft)
            assertEquals(0, a.coverIndex)
        }
        f.delete()
    }

    @Test
    fun realLyraChapterOne() {
        val f = File("/workspace/comic/exports/books/Lyra_Ch1.cbz")
        assumeTrue("sample CBZ not present", f.exists())
        CbzArchive(f).use { a ->
            assertEquals(7, a.pageCount)
            assertEquals((1..7).map { "%03d.png".format(it) }, a.pages.map { it.name })
            val info = assertNotNullAndGet(a.info)
            assertEquals("Lyra — Chapter 1", info.title)
            assertEquals("Lyra", info.series)
            assertEquals("1", info.number)
            assertEquals(7, info.pageCount)
            assertEquals(0, info.frontCoverIndex)
            assertFalse(info.rightToLeft)
            // PNG signature of the first page.
            val head = a.readBytes(0).copyOfRange(0, 8)
            assertEquals(0x89.toByte(), head[0]); assertEquals('P'.code.toByte(), head[1])
            // 864x1152 straight from the IHDR chunk.
            val b = a.readBytes(0)
            val w = ((b[16].toInt() and 0xff) shl 24) or ((b[17].toInt() and 0xff) shl 16) or ((b[18].toInt() and 0xff) shl 8) or (b[19].toInt() and 0xff)
            val h = ((b[20].toInt() and 0xff) shl 24) or ((b[21].toInt() and 0xff) shl 16) or ((b[22].toInt() and 0xff) shl 8) or (b[23].toInt() and 0xff)
            assertEquals(864, w); assertEquals(1152, h)
        }
    }

    @Test
    fun allLyraChaptersParse() {
        for (n in 1..4) {
            val f = File("/workspace/comic/exports/books/Lyra_Ch$n.cbz")
            assumeTrue(f.exists())
            CbzArchive(f).use { a ->
                assertEquals(a.info?.pageCount, a.pageCount)
                assertEquals("$n", a.info?.number)
                assertEquals("Lyra", a.info?.series)
            }
        }
    }

    @Test
    fun fileNameHeuristics() {
        NameHeuristics.guess("Lyra_Ch1.pdf").let {
            assertEquals("Lyra", it.series); assertEquals("1", it.number); assertEquals("Lyra — Chapter 1", it.title)
        }
        NameHeuristics.guess("Saga Vol 03.cbz").let {
            assertEquals("Saga", it.series); assertEquals("3", it.number); assertEquals("Volume", it.label)
        }
        NameHeuristics.guess("Blazarbound #12.cbz").let { assertEquals("Blazarbound", it.series); assertEquals("Issue", it.label) }
        NameHeuristics.guess("One Shot.cbz").let { assertEquals(null, it.series); assertEquals("One Shot", it.title) }
    }

    @Test
    fun spreadsKeepCoverAndDoublePagesAlone() {
        val s = buildSpreads(8, coverIndex = 0, doublePages = setOf(4)).map { it.toList() }
        assertEquals(listOf(listOf(0), listOf(1, 2), listOf(3), listOf(4), listOf(5, 6), listOf(7)), s)
        val units = buildSpreads(7)
        assertEquals(listOf(listOf(0), listOf(1, 2), listOf(3, 4), listOf(5, 6)), units.map { it.toList() })
        assertEquals(2, units.unitOf(4))
    }

    private fun <T> assertNotNullAndGet(v: T?): T { assertNotNull(v); return v!! }
}
