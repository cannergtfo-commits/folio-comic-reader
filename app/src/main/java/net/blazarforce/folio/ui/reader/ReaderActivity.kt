package net.blazarforce.folio.ui.reader

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import net.blazarforce.folio.R
import net.blazarforce.folio.comic.PageLoader
import net.blazarforce.folio.comic.PageSource
import net.blazarforce.folio.data.Importer
import net.blazarforce.folio.data.nextInSeries
import net.blazarforce.folio.folio
import net.blazarforce.folio.ui.theme.FolioTheme
import kotlin.math.max
import kotlin.math.min

/** Immersive, screen-awake reading surface for one book. */
class ReaderActivity : ComponentActivity() {
    private var source: PageSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val app = folio
        val id = intent.getStringExtra(EXTRA_BOOK)
        val book = id?.let { app.store.get(it) }
        if (book == null) { finish(); return }
        val src = runCatching { Importer.openSource(this, book) }.getOrElse {
            Toast.makeText(this, "Couldn't open this book.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        source = src
        val dm = resources.displayMetrics
        val short = min(dm.widthPixels, dm.heightPixels)
        val long = max(dm.widthPixels, dm.heightPixels)
        val loader = PageLoader(src, short, long, lifecycleScope, cacheFraction = 5)
        val thumbs = PageLoader(src, 180, 240, lifecycleScope, cacheFraction = 24)
        val start = if (intent.getBooleanExtra(EXTRA_FROM_START, false)) 0
        else if (book.finished) 0 else book.lastPage

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val settings by app.prefs.settings.collectAsState()
            val books by app.store.books.collectAsState()
            val current = books.firstOrNull { it.id == book.id } ?: book
            val next = remember(books, current) { books.nextInSeries(current) }
            LaunchedEffect(settings.brightness) {
                window.attributes = window.attributes.apply {
                    screenBrightness = if (settings.brightness < 0f) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    else settings.brightness
                }
            }
            FolioTheme(settings.theme) {
                ReaderScreen(
                    book = current, loader = loader, thumbs = thumbs, settings = settings, nextBook = next,
                    initialPage = start,
                    onPageChanged = { page -> app.store.saveProgress(book.id, page) },
                    onBack = { finishWithFade() },
                    onSettings = { app.prefs.update(it) },
                    onSetRtl = { v -> app.store.update(book.id) { it.copy(rtlOverride = v) } },
                    onOpenNext = { n ->
                        startActivity(
                            Intent(this, ReaderActivity::class.java).putExtra(EXTRA_BOOK, n.id).putExtra(EXTRA_FROM_START, true),
                        )
                        @Suppress("DEPRECATION")
                        overridePendingTransition(R.anim.folio_fade_in, R.anim.folio_fade_out)
                        finish()
                    },
                    onUiVisible = { visible ->
                        if (visible) controller.show(WindowInsetsCompat.Type.systemBars())
                        else controller.hide(WindowInsetsCompat.Type.systemBars())
                    },
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun finishWithFade() {
        finish()
        overridePendingTransition(R.anim.folio_hold, R.anim.folio_fade_out)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.folio_hold, R.anim.folio_fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        source?.close()
    }

    companion object {
        const val EXTRA_BOOK = "book"
        const val EXTRA_FROM_START = "fromStart"
    }
}
