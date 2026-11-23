package net.blazarforce.folio

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import net.blazarforce.folio.data.BookKind
import net.blazarforce.folio.data.Catalog
import net.blazarforce.folio.data.CatalogRepo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The published catalog parses, and the live pipeline (fetch → download → shelf → offline cache) works. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CatalogTest {
    private val local = File("/workspace/comic-reader-app/catalog/catalog.json")

    @org.junit.Before fun setUp() {
        net.blazarforce.folio.data.Importer.io = kotlinx.coroutines.Dispatchers.Unconfined
        android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
            .compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, java.io.ByteArrayOutputStream())
    }

    @Test
    fun localCatalogParses() {
        assumeTrue(local.exists())
        val c = Catalog.parse(local.readText())
        assertEquals(1, c.series.size)
        val s = c.series.first()
        assertEquals("lyra-bw", s.id)
        assertEquals("Lyra", s.title)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), s.chapters.map { it.number }.take(6))
        assertEquals(listOf(7, 9, 9, 13, 13, 13), s.chapters.map { it.pages }.take(6))
        s.chapters.forEach {
            assertTrue(it.cbz.startsWith("https://github.com/cannergtfo-commits/folio-comic-reader/releases/download/library/"))
            assertTrue(it.size > 1_000_000)
            assertNotNull(it.cover)
        }
        // Tolerates unknown fields and skips broken entries.
        val odd = Catalog.parse("""{"schema":1,"future":true,"series":[{"id":"x","title":"X","chapters":[{"id":"bad"},{"id":"ok","number":1,"title":"One","cbz":"https://e/x/One.cbz"}]}]}""")
        assertEquals(listOf("ok"), odd.chapters.map { it.id })
        assertEquals("One.cbz", odd.chapters.first().fileName)
    }

    @Test
    fun liveFetchDownloadAndOffline() {
        val app = ApplicationProvider.getApplicationContext<FolioApp>()
        val repo = CatalogRepo(app, app.store, app.importer)
        runBlocking { repo.refresh() }
        val st = repo.state.value
        assumeTrue("network", st.error == null)
        val cat = st.catalog!!
        assertTrue(cat.chapters.size >= 6)
        val ch1 = cat.chapters.first { it.id == "lyra-bw-1" }
        assertEquals(6, repo.newChapters().size.coerceAtMost(6))
        assertTrue("covers cached", st.covers.isNotEmpty() || repo.state.value.covers.isNotEmpty())

        val book = runBlocking { repo.download(ch1) }.getOrThrow()
        assertEquals(ch1.bookId, book.id)
        assertEquals(BookKind.CBZ, book.kind)
        assertEquals("Lyra (B&W edition)", book.series)
        assertEquals("1", book.number)
        assertEquals(7, book.pageCount)
        assertEquals(ch1.size, File(book.location).length())
        assertTrue(repo.isOnShelf(ch1))
        assertTrue(repo.newChapters().none { it.id == ch1.id })
        // Downloading again is a no-op.
        assertEquals(book.id, runBlocking { repo.download(ch1) }.getOrThrow().id)
        assertEquals(1, app.store.books.value.size)

        // Offline: a repo pointed at an unreachable URL still shows the cached catalog and covers.
        val offline = CatalogRepo(app, app.store, app.importer, url = "https://127.0.0.1:9/catalog.json")
        assertEquals(cat.chapters.size, offline.state.value.catalog!!.chapters.size)
        assertTrue(offline.state.value.covers.isNotEmpty())
        runBlocking { offline.refresh() }
        assertEquals("offline", offline.state.value.error)
        assertNotNull(offline.state.value.catalog)
        assertNull(repo.state.value.error)
    }
}
