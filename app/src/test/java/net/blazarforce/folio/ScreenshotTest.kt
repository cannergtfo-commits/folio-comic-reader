package net.blazarforce.folio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
import net.blazarforce.folio.data.ReadingMode
import net.blazarforce.folio.data.Settings
import net.blazarforce.folio.data.ShelfItem
import net.blazarforce.folio.data.buildShelf
import net.blazarforce.folio.ui.common.CoverCache
import net.blazarforce.folio.ui.library.LibraryScreen
import net.blazarforce.folio.ui.library.SeriesScreen
import net.blazarforce.folio.ui.reader.ReaderScreen
import net.blazarforce.folio.ui.reader.ReaderSettingsContent
import net.blazarforce.folio.ui.theme.FolioTheme
import net.blazarforce.folio.ui.theme.LocalPalette
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.performScrollToIndex
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real Compose UI (Robolectric native graphics) with the real Lyra CBZ pages
 * and writes PNGs to /workspace/comic-reader-app/screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val books = File("/workspace/comic/exports/books")
    private val out = File("/workspace/comic-reader-app/screenshots")
    private val tmp = File(System.getProperty("java.io.tmpdir"), "folio-shots").apply { mkdirs() }

    private fun cover(id: String, bmp: Bitmap): String {
        val path = File(tmp, "$id.jpg").absolutePath
        val w = 480
        val scaled = Bitmap.createScaledBitmap(bmp, w, bmp.height * w / bmp.width, true)
        CoverCache.put(path, scaled)
        return path
    }

    private fun library(): List<Book> {
        assumeTrue(books.exists())
        val now = System.currentTimeMillis()
        val lyra = (1..4).map { n ->
            val f = File(books, "Lyra_Ch$n.cbz")
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
        val bbCover = File("/workspace/blazarshootem/comics/issue01_cover_lettered.png")
        val extra = if (bbCover.exists()) listOf(
            Book(
                id = "bb1", kind = BookKind.PDF, location = "", title = "Blazar Bound — Issue 1", series = null,
                number = null, pageCount = 24, coverPath = cover("bb1", BitmapFactory.decodeFile(bbCover.absolutePath)),
                addedAt = now - 7 * 86_400_000L,
            ),
        ) else emptyList()
        return lyra + extra
    }

    private fun snap(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(out, name).absolutePath)
    }

    private fun libraryShot(theme: AppTheme, name: String) {
        val list = library()
        compose.setContent {
            FolioTheme(theme) {
                LibraryScreen(list, theme, false, {}, {}, {}, {}, {}, {}, hour = 20)
            }
        }
        snap(name)
    }

    @Test fun libraryInk() = libraryShot(AppTheme.INK, "01_library_ink.png")

    @Test fun libraryPaper() = libraryShot(AppTheme.PAPER, "02_library_paper.png")

    @Test
    fun seriesInk() {
        val series = buildShelf(library()).filterIsInstance<ShelfItem.Series>().first()
        compose.setContent { FolioTheme(AppTheme.INK) { SeriesScreen(series, {}, {}, {}) } }
        snap("03_series_ink.png")
    }

    @Test
    fun emptyInk() {
        compose.setContent { FolioTheme(AppTheme.INK) { LibraryScreen(emptyList(), AppTheme.INK, false, {}, {}, {}, {}, {}, {}) } }
        snap("04_empty_ink.png")
    }

    private fun readerShot(name: String, settings: Settings, page: Int, ui: Boolean, theme: AppTheme = AppTheme.INK) {
        val book = library()[1]
        val src = CbzPageSource(File(book.location))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val loader = PageLoader(src, 1080, 2340, scope)
        val thumbs = PageLoader(src, 180, 240, scope)
        runBlocking {
            for (i in 0 until src.pageCount) { loader.load(i); thumbs.load(i) }
        }
        compose.setContent {
            FolioTheme(theme) {
                ReaderScreen(
                    book = book, loader = loader, thumbs = thumbs, settings = settings, nextBook = null,
                    onPageChanged = {}, onBack = {}, onSettings = {}, onSetRtl = {}, onOpenNext = {}, onUiVisible = {},
                    initialPage = page, initialUi = ui, autoHideUi = false,
                )
            }
        }
        snap(name)
    }

    @Test fun readerOverlay() = readerShot("05_reader_overlay.png", Settings(haptics = false), 3, ui = true)

    @Test fun readerClean() = readerShot("06_reader_immersive.png", Settings(haptics = false), 3, ui = false)

    @Test fun readerWarm() = readerShot("07_reader_warm_filter.png", Settings(haptics = false, warmth = 0.7f), 5, ui = false)

    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    @Test fun readerSpread() = readerShot("08_reader_spread_landscape.png", Settings(haptics = false, readingMode = ReadingMode.PAGED, autoSpreadLandscape = true), 1, ui = true)

    @Test
    fun readerSettings() {
        compose.setContent {
            FolioTheme(AppTheme.INK) {
                val p = LocalPalette.current
                Box(Modifier.fillMaxSize().background(Color(0xFF0A0908))) {
                    Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(p.surface),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.width(32.dp).height(4.dp).background(p.hairline, CircleShape))
                        }
                        ReaderSettingsContent(
                            Settings(pageTurn = PageTurn.FADE, warmth = 0.35f, brightness = -1f), rtl = false, onSettings = {}, onSetRtl = {},
                        )
                    }
                }
            }
        }
        snap("09_reader_settings.png")
    }

    private fun discoverState(): Pair<net.blazarforce.folio.data.LibraryState, Set<String>> {
        val f = File("/workspace/comic-reader-app/catalog/catalog.json")
        assumeTrue(f.exists())
        val cat = net.blazarforce.folio.data.Catalog.parse(f.readText())
        val covers = cat.chapters.mapNotNull { ch ->
            val u = ch.cover ?: return@mapNotNull null
            val local = File("/workspace/comic-reader-app/catalog/covers", u.substringAfterLast('/'))
            CoverCache.put(local.absolutePath, BitmapFactory.decodeFile(local.absolutePath))
            u to local.absolutePath
        }.toMap()
        val ch = cat.chapters
        val state = net.blazarforce.folio.data.LibraryState(
            catalog = cat, fetchedAt = System.currentTimeMillis() - 4 * 60_000,
            seen = ch.take(3).map { it.id }.toSet(), covers = covers,
            downloads = mapOf(ch[2].id to 0.62f),
        )
        return state to ch.take(2).map { it.bookId }.toSet()
    }

    private fun discoverShot(theme: AppTheme, name: String, scrollTo: Int = 0) {
        val (state, shelf) = discoverState()
        compose.setContent {
            FolioTheme(theme) {
                Box(Modifier.fillMaxSize()) {
                    net.blazarforce.folio.ui.discover.DiscoverScreen(state, shelf, null, {}, {}, {}, {}, {})
                    net.blazarforce.folio.ui.discover.TabSwitch(
                        true, 3, {}, {},
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
                    )
                }
            }
        }
        if (scrollTo > 0) compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToIndex(scrollTo)
        snap(name)
    }

    @Test fun discoverInk() = discoverShot(AppTheme.INK, "13_discover_ink.png")

    @Test fun discoverInkChapters() = discoverShot(AppTheme.INK, "14_discover_chapters_ink.png", scrollTo = 3)

    @Test fun discoverPaper() = discoverShot(AppTheme.PAPER, "15_discover_paper.png")

    @Test
    fun discoverOfflineEmpty() {
        compose.setContent {
            FolioTheme(AppTheme.INK) {
                net.blazarforce.folio.ui.discover.DiscoverScreen(
                    net.blazarforce.folio.data.LibraryState(error = "offline-empty"), emptySet(), null, {}, {}, {}, {}, {},
                )
            }
        }
        snap("16_discover_offline.png")
    }

    @Test
    fun appIcon() {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF2B2F36)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconShape(CircleShape, 120)
                        IconShape(RoundedCornerShape(28), 120)
                    }
                    IconShape(RoundedCornerShape(24), 220)
                    Text("Folio", color = Color(0xFFEFE8D8), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        snap("10_app_icon.png")
    }

    @Composable
    private fun IconShape(shape: androidx.compose.ui.graphics.Shape, size: Int) {
        // Adaptive icons show roughly the middle 72/108 of each layer.
        Box(Modifier.size(size.dp).shadow(12.dp, shape).clip(shape), contentAlignment = Alignment.Center) {
            val layer = Modifier.requiredSize((size * 1.5f).dp)
            Image(painterResource(R.drawable.ic_launcher_background), null, layer)
            Image(painterResource(R.drawable.ic_launcher_foreground), null, layer)
        }
    }
}
