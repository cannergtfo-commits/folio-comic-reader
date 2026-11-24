package net.blazarforce.folio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import net.blazarforce.folio.comic.CbzArchive
import net.blazarforce.folio.comic.CbzPageSource
import net.blazarforce.folio.comic.PageLoader
import net.blazarforce.folio.data.AppTheme
import net.blazarforce.folio.data.Book
import net.blazarforce.folio.data.BookKind
import net.blazarforce.folio.data.PageTurn
import net.blazarforce.folio.data.Settings
import net.blazarforce.folio.ui.common.CoverCache
import net.blazarforce.folio.ui.common.FolioMotion
import net.blazarforce.folio.ui.discover.DiscoverScreen
import net.blazarforce.folio.ui.discover.TabSwitch
import net.blazarforce.folio.ui.library.AboutPrivacyContent
import net.blazarforce.folio.ui.library.LibraryScreen
import net.blazarforce.folio.ui.reader.ReaderScreen
import net.blazarforce.folio.ui.theme.FolioTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 9:16 (360×640 dp at xxhdpi = 1080×1920 px) Play Store phone screenshots.
 * Written to FOLIO_PLAY_SHOTS, then flattened to 24-bit RGB by the kit script.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
class PlayStoreScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val booksDir = File(System.getenv("FOLIO_TESTDATA") ?: "/workspace/folio-testdata/books")
    private val out = File(System.getenv("FOLIO_PLAY_SHOTS") ?: "/workspace/comic-reader-app/play-store-kit/raw-shots").apply { mkdirs() }
    private val tmp = File(System.getProperty("java.io.tmpdir"), "folio-play-shots").apply { mkdirs() }
    private val still = FolioMotion(ambient = false)

    private fun cover(id: String, bmp: Bitmap): String {
        val path = File(tmp, "$id.jpg").absolutePath
        val w = 480
        val scaled = Bitmap.createScaledBitmap(bmp, w, bmp.height * w / bmp.width, true)
        CoverCache.put(path, scaled)
        return path
    }

    private fun library(): List<Book> {
        assumeTrue(booksDir.exists())
        val now = System.currentTimeMillis()
        return (1..4).map { n ->
            val f = File(booksDir, "Lyra_Ch${n}_BW.cbz")
            CbzArchive(f).use { a ->
                val bytes = a.readBytes(a.coverIndex)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Book(
                    id = "lyra$n", kind = BookKind.CBZ, location = f.absolutePath,
                    title = a.info?.title ?: "Lyra $n", series = "Lyra", number = "$n", numberLabel = "Chapter",
                    pageCount = a.pageCount, coverPath = cover("lyra$n", bmp),
                    lastPage = when (n) { 1 -> a.pageCount - 1; 2 -> 3; else -> 0 },
                    finished = n == 1,
                    lastReadAt = when (n) { 1 -> now - 86_400_000; 2 -> now - 3_600_000; else -> 0 },
                    addedAt = now - 5 * 86_400_000L,
                )
            }
        }
    }

    private fun snap(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(out, name).absolutePath)
    }

    private fun discoverState(): Pair<net.blazarforce.folio.data.LibraryState, Set<String>> {
        val f = File("/workspace/comic-reader-app/catalog/catalog.json")
        assumeTrue(f.exists())
        val cat = net.blazarforce.folio.data.Catalog.parse(f.readText())
        val covers = cat.chapters.mapNotNull { ch ->
            val u = ch.cover ?: return@mapNotNull null
            val local = File("/workspace/comic-reader-app/catalog/covers", u.substringAfterLast('/'))
            if (local.exists()) CoverCache.put(local.absolutePath, BitmapFactory.decodeFile(local.absolutePath))
            u to local.absolutePath
        }.toMap()
        val ch = cat.chapters
        val state = net.blazarforce.folio.data.LibraryState(
            catalog = cat, fetchedAt = System.currentTimeMillis() - 4 * 60_000,
            seen = ch.take(3).map { it.id }.toSet(), covers = covers,
        )
        return state to ch.take(2).map { it.bookId }.toSet()
    }

    @Test
    fun discoverComics() {
        val (state, shelf) = discoverState()
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = still) {
                Box(Modifier.fillMaxSize()) {
                    DiscoverScreen(state, shelf, null, {}, {}, {}, {}, {}, openSeries = null)
                    TabSwitch(true, 3, {}, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
                }
            }
        }
        snap("01_discover_comics.png")
    }

    @Test
    fun libraryShelf() {
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = still) {
                Box(Modifier.fillMaxSize()) {
                    LibraryScreen(library(), AppTheme.INK, false, {}, {}, {}, {}, {}, {}, onDiscover = {})
                    TabSwitch(false, 3, {}, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
                }
            }
        }
        snap("02_library_shelf.png")
    }

    @Test
    fun seriesChapters() {
        val (state, shelf) = discoverState()
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = still) {
                DiscoverScreen(state, shelf, null, {}, {}, {}, {}, {}, openSeries = "lyra-bw")
            }
        }
        snap("03_series_chapters.png")
    }

    @Test
    fun readerSettings() {
        val book = library()[1]
        val src = CbzPageSource(File(book.location))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val loader = PageLoader(src, 1080, 1920, scope)
        val thumbs = PageLoader(src, 180, 240, scope)
        runBlocking {
            for (i in 0 until src.pageCount) { loader.load(i); thumbs.load(i) }
        }
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = still) {
                ReaderScreen(
                    book = book, loader = loader, thumbs = thumbs,
                    settings = Settings(pageTurn = PageTurn.FADE, warmth = 0.35f, brightness = -1f, haptics = false),
                    nextBook = null, onPageChanged = {}, onBack = {}, onSettings = {}, onSetRtl = {}, onOpenNext = {},
                    onUiVisible = {}, initialPage = 3, initialUi = true, autoHideUi = false, initialSheet = true,
                )
            }
        }
        snap("04_reader_settings.png")
    }

    @Test
    fun aboutPrivacy() {
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = still) {
                Box(Modifier.fillMaxSize()) {
                    LibraryScreen(library(), AppTheme.INK, false, {}, {}, {}, {}, {}, {}, onDiscover = {})
                    TabSwitch(false, 3, {}, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AboutPrivacyContent(BuildConfig.VERSION_NAME, {}, {}, Modifier.padding(horizontal = 18.dp))
                    }
                }
            }
        }
        snap("05_about_privacy.png")
    }
}
