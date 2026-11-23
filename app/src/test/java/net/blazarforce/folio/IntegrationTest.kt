package net.blazarforce.folio

import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import net.blazarforce.folio.data.BookKind
import net.blazarforce.folio.ui.reader.ReaderActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** End-to-end through the real Importer, LibraryStore and activities (Robolectric). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class IntegrationTest {
    private val books = File(System.getenv("FOLIO_TESTDATA") ?: "/workspace/folio-testdata/books")
    private val out = File(System.getenv("FOLIO_SHOTS") ?: "/workspace/comic-reader-app/screenshots")

    @org.junit.Before fun setUp() {
        net.blazarforce.folio.ui.common.FolioMotion.ambientByDefault = false
        net.blazarforce.folio.data.Importer.io = kotlinx.coroutines.Dispatchers.Unconfined
        // Prime Robolectric's native graphics JNI on the test thread.
        android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
            .compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, java.io.ByteArrayOutputStream())
        if (books.exists()) net.blazarforce.folio.comic.CbzArchive(File(books, "Lyra_Ch1_BW.cbz")).use { a ->
            val b = a.readBytes(0)
            val bmp = android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size)
            val small = android.graphics.Bitmap.createScaledBitmap(bmp, 120, 160, true)
            val jpg = java.io.ByteArrayOutputStream().also { small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(jpg, 0, jpg.size)
        }
    }

    private fun idle() { repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(50) } }

    @Test
    fun importAllLyraChaptersThenOpenLibraryAndReader() {
        assumeTrue(books.exists())
        val app = ApplicationProvider.getApplicationContext<FolioApp>()
        val imported = runBlocking {
            (1..4).map { n -> app.importer.importDocument(Uri.fromFile(File(books, "Lyra_Ch${n}_BW.cbz")))!! }
        }
        imported.forEachIndexed { i, b ->
            assertEquals(BookKind.CBZ, b.kind)
            assertEquals("Lyra (B&W edition)", b.series)
            assertEquals("${i + 1}", b.number)
            assertTrue(b.title.startsWith("Chapter ${i + 1}: "))
            assertTrue(File(b.location).length() > 1_000_000)
            assertTrue("cover written", File(b.coverPath!!).length() > 10_000)
        }
        assertEquals(listOf(7, 9, 9, 13), imported.map { it.pageCount })
        // Re-importing the same file is de-duplicated.
        val again = runBlocking { app.importer.importDocument(Uri.fromFile(File(books, "Lyra_Ch1_BW.cbz"))) }
        assertEquals(imported[0].id, again!!.id)
        assertEquals(4, app.store.books.value.size)

        // A PDF goes through PdfRenderer-free metadata path only if the platform can render; just check detection.
        app.store.saveProgress(imported[1].id, 2)
        assertTrue(app.store.get(imported[1].id)!!.inProgress)

        app.store.books.value.forEach { net.blazarforce.folio.ui.common.CoverCache.load(it.coverPath!!) }
        val main = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        idle()
        main.window.decorView.captureRoboImage(File(out, "11_app_library_after_import.png").absolutePath)

        val intent = Intent(ApplicationProvider.getApplicationContext(), ReaderActivity::class.java)
            .putExtra(ReaderActivity.EXTRA_BOOK, imported[1].id)
        val reader = Robolectric.buildActivity(ReaderActivity::class.java, intent).setup().get()
        idle()
        Thread.sleep(800); idle()
        reader.window.decorView.captureRoboImage(File(out, "12_app_reader_live.png").absolutePath)
        assertTrue(reader.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
    }

    @Test
    fun viewIntentImportsAndLaunchesReader() {
        assumeTrue(books.exists())
        val intent = Intent(Intent.ACTION_VIEW, Uri.fromFile(File(books, "Lyra_Ch3_BW.cbz")))
            .setClass(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val main = Robolectric.buildActivity(MainActivity::class.java, intent).setup().get()
        var next: Intent? = null
        for (k in 0 until 60) {
            idle()
            next = shadowOf(main).nextStartedActivity
            if (next != null) break
        }
        assertNotNull("reader launched for Open-with intent", next)
        assertEquals(ReaderActivity::class.java.name, next!!.component!!.className)
        val app = ApplicationProvider.getApplicationContext<FolioApp>()
        assertSame(1, app.store.books.value.size)
        assertEquals("Chapter 3: The Last Purebloods", app.store.books.value.first().title)
    }
}
