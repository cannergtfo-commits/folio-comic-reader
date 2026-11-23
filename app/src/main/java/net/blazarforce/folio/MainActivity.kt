package net.blazarforce.folio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.blazarforce.folio.data.AppTheme
import net.blazarforce.folio.data.Book
import net.blazarforce.folio.data.ShelfItem
import net.blazarforce.folio.data.buildShelf
import net.blazarforce.folio.ui.discover.DiscoverScreen
import net.blazarforce.folio.ui.discover.TabSwitch
import net.blazarforce.folio.ui.library.BookSheet
import net.blazarforce.folio.ui.library.LibraryScreen
import net.blazarforce.folio.ui.library.SeriesScreen
import net.blazarforce.folio.ui.reader.ReaderActivity
import net.blazarforce.folio.ui.theme.FolioTheme
import net.blazarforce.folio.ui.theme.InkPalette
import net.blazarforce.folio.ui.theme.LocalPalette
import net.blazarforce.folio.ui.theme.PaperPalette

class MainActivity : ComponentActivity() {
    private val messages = Channel<String>(Channel.BUFFERED)
    private var importing by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = folio
        applyBars(app.prefs.settings.value.theme)

        setContent {
            val settings by app.prefs.settings.collectAsState()
            val books by app.store.books.collectAsState()
            val lib by app.catalog.state.collectAsState()
            var discover by rememberSaveable { mutableStateOf(false) }
            val shelfIds = remember(books) { books.map { it.id }.toSet() }
            val newCount = lib.catalog?.chapters.orEmpty().count { it.id !in lib.seen && it.bookId !in shelfIds }
            val update = lib.catalog?.app?.takeIf { it.versionCode > BuildConfig.VERSION_CODE && it.apk.isNotBlank() }
            var seriesName by rememberSaveable { mutableStateOf<String?>(null) }
            var sheetFor by remember { mutableStateOf<Book?>(null) }
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            LaunchedEffect(settings.theme) { applyBars(settings.theme) }
            LaunchedEffect(Unit) { messages.receiveAsFlow().collect { snackbar.showSnackbar(it) } }
            LaunchedEffect(Unit) {
                val fresh = app.catalog.refresh().filter { it.bookId !in app.store.books.value.map { b -> b.id } }
                if (fresh.isNotEmpty() && !discover) {
                    messages.send(if (fresh.size == 1) "New in Discover: ${fresh.first().seriesTitle}, chapter ${fresh.first().numberLabel}" else "${fresh.size} new chapters in Discover")
                }
            }

            val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                if (uris.isNotEmpty()) importAll(uris)
            }
            val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) importFolder(uri)
            }
            val openFiles = { pickFiles.launch(MIME_TYPES) }
            val openFolder = { pickFolder.launch(null) }

            FolioTheme(settings.theme) {
                val p = LocalPalette.current
                val series = seriesName?.let { name ->
                    buildShelf(books).filterIsInstance<ShelfItem.Series>().firstOrNull { it.name == name }
                }
                BackHandler(series != null) { seriesName = null }
                BackHandler(series == null && discover) { discover = false }
                Box(Modifier.fillMaxSize().background(p.canvas)) {
                    AnimatedContent(
                        targetState = discover && series == null,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                        label = "tab",
                    ) { onDiscover ->
                    if (onDiscover) {
                        DisposableEffect(Unit) {
                            onDispose { app.catalog.state.value.catalog?.let { c -> app.catalog.markSeen(c.chapters.map { it.id }) } }
                        }
                        DiscoverScreen(
                            state = lib, shelfIds = shelfIds, update = update,
                            onRefresh = { scope.launch { app.catalog.refresh() } },
                            onGet = { ch -> scope.launch { getChapters(listOf(ch)) } },
                            onGetAll = { s -> scope.launch { getChapters(s.chapters.filter { it.bookId !in shelfIds }) } },
                            onRead = { ch -> app.catalog.bookFor(ch)?.let { openReader(it) } },
                            onUpdate = { a -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.apk))) } },
                        )
                    } else {
                    AnimatedContent(
                        targetState = series,
                        transitionSpec = {
                            val forward = targetState != null
                            (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { if (forward) it / 10 else -it / 10 }) togetherWith
                                (fadeOut(tween(200)) + slideOutHorizontally(tween(260)) { if (forward) -it / 10 else it / 10 })
                        },
                        contentKey = { it?.name },
                        label = "nav",
                    ) { s ->
                        if (s == null) {
                            LibraryScreen(
                                books = books, theme = settings.theme, importing = importing,
                                onToggleTheme = {
                                    app.prefs.update { it.copy(theme = if (it.theme == AppTheme.INK) AppTheme.PAPER else AppTheme.INK) }
                                },
                                onImportFiles = openFiles, onImportFolder = openFolder,
                                onOpen = { openReader(it) },
                                onOpenSeries = { seriesName = it },
                                onLongPress = { sheetFor = it },
                                onDiscover = { discover = true },
                            )
                        } else {
                            SeriesScreen(s, onBack = { seriesName = null }, onOpen = { openReader(it) }, onLongPress = { sheetFor = it })
                        }
                    }
                    }
                    }
                    if (series == null) {
                        TabSwitch(
                            discover = discover, newCount = newCount,
                            onShelf = { discover = false }, onDiscover = { discover = true },
                            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp),
                        )
                    }
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = if (series == null) 76.dp else 16.dp)) { data ->
                        Snackbar(
                            containerColor = p.raised, contentColor = p.text, shape = RoundedCornerShape(12.dp),
                        ) { Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                sheetFor?.let { b ->
                    val book = books.firstOrNull { it.id == b.id } ?: return@let
                    BookSheet(
                        book,
                        onDismiss = { sheetFor = null },
                        onRead = { sheetFor = null; openReader(book) },
                        onFromStart = { sheetFor = null; openReader(book, fromStart = true) },
                        onToggleFinished = { app.store.setFinished(book.id, !book.finished); sheetFor = null },
                        onRemove = {
                            sheetFor = null
                            scope.launch { app.store.remove(book.id); messages.send("Removed “${book.title}”") }
                        },
                    )
                }
            }
        }
        handleViewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun applyBars(theme: AppTheme) {
        val c = (if (theme == AppTheme.INK) InkPalette else PaperPalette).canvas.toArgb()
        val style = if (theme == AppTheme.INK) SystemBarStyle.dark(c) else SystemBarStyle.light(c, c)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun handleViewIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        intent.data = null
        lifecycleScope.launch {
            importing = true
            val book = runCatching { folio.importer.importDocument(uri) }.getOrNull()
            importing = false
            if (book != null) openReader(book) else messages.send("That file isn't a CBZ or PDF Folio can read.")
        }
    }

    /** Downloads catalog chapters one after another onto the shelf. */
    private suspend fun getChapters(list: List<net.blazarforce.folio.data.CatalogChapter>) {
        var ok = 0
        var failed = 0
        var last: Book? = null
        for (ch in list) {
            folio.catalog.download(ch).onSuccess { ok++; last = it }.onFailure { if (it !is IllegalStateException) failed++ }
        }
        when {
            failed > 0 && ok == 0 -> messages.send("Couldn't download right now. Check your connection and try again.")
            failed > 0 -> messages.send("Added $ok · $failed didn't download")
            ok == 1 -> messages.send("“${last!!.title}” is on your shelf")
            ok > 1 -> messages.send("Added $ok chapters to your shelf")
        }
    }

    private fun importAll(uris: List<Uri>) = lifecycleScope.launch {
        importing = true
        var ok = 0
        uris.forEach { if (runCatching { folio.importer.importDocument(it) }.getOrNull() != null) ok++ }
        importing = false
        val failed = uris.size - ok
        messages.send(
            when {
                ok == 0 -> "Nothing imported — Folio reads CBZ and PDF files."
                failed > 0 -> "Added $ok · skipped $failed unsupported"
                ok == 1 -> "Added to your shelf"
                else -> "Added $ok books to your shelf"
            },
        )
    }

    private fun importFolder(uri: Uri) = lifecycleScope.launch {
        importing = true
        val books = runCatching { folio.importer.importTree(uri) }.getOrDefault(emptyList())
        importing = false
        messages.send(if (books.isEmpty()) "No comics found in that folder." else "Added ${books.size} from the folder")
    }

    private fun openReader(book: Book, fromStart: Boolean = false) {
        val intent = Intent(this, ReaderActivity::class.java)
            .putExtra(ReaderActivity.EXTRA_BOOK, book.id)
            .putExtra(ReaderActivity.EXTRA_FROM_START, fromStart)
        val opts = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.folio_fade_in, R.anim.folio_hold)
        startActivity(intent, opts.toBundle())
    }

    companion object {
        val MIME_TYPES = arrayOf(
            "application/pdf", "application/vnd.comicbook+zip", "application/x-cbz",
            "application/zip", "application/x-zip-compressed", "application/octet-stream",
        )
    }
}
