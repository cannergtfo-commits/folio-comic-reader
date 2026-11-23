package net.blazarforce.folio

import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.blazarforce.folio.data.Catalog
import net.blazarforce.folio.data.LibraryState
import net.blazarforce.folio.ui.common.CoverCache
import net.blazarforce.folio.ui.common.FolioMotion
import net.blazarforce.folio.ui.discover.DiscoverScreen
import net.blazarforce.folio.data.AppTheme
import net.blazarforce.folio.ui.theme.FolioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Discover lists comics first; tapping one opens its chapters, Back returns to the list. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DiscoverNavigationTest {
    @get:Rule val compose = createComposeRule()

    private fun state(): LibraryState {
        val f = File("/workspace/comic-reader-app/catalog/catalog.json")
        assumeTrue(f.exists())
        return LibraryState(catalog = Catalog.parse(f.readText()), fetchedAt = System.currentTimeMillis())
    }

    private var open by mutableStateOf<String?>(null)

    private fun show(reduce: Boolean = false) {
        val s = state()
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = FolioMotion(reduce = reduce, ambient = false)) {
                DiscoverScreen(s, emptySet(), null, {}, {}, {}, {}, {}, openSeries = open, onOpenSeries = { open = it })
            }
        }
    }

    @Test
    fun tapComicOpensItsChaptersAndBackReturns() {
        val cat = state().catalog!!
        val lyra = cat.series.first()
        val firstChapter = lyra.chapters.first().title.substringAfter(": ")
        show()
        // Top level shows comic cards only, not chapter rows.
        compose.onNodeWithTag("discover-list").assertIsDisplayed()
        cat.series.forEach { compose.onNodeWithTag("series:" + it.id, useUnmergedTree = true).assertExists() }
        compose.onNodeWithText(firstChapter).assertDoesNotExist()

        compose.onNodeWithTag("series:" + lyra.id).performClick()
        compose.waitForIdle()
        assertEquals(lyra.id, open)
        compose.onNodeWithTag("discover-series").assertIsDisplayed()
        compose.onNodeWithText(firstChapter).assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()
        assertNull(open)
        compose.onNodeWithTag("discover-list").assertIsDisplayed()
        compose.onNodeWithText(firstChapter).assertDoesNotExist()
    }

    @Test
    fun deepLinkOpensTheRightSeriesWithReducedMotion() {
        val cat = state().catalog!!
        val other = cat.series.last()
        open = other.id
        show(reduce = true)
        compose.onNodeWithTag("discover-series").assertIsDisplayed()
        compose.onNodeWithText(other.chapters.first().title.substringAfter(": ")).assertExists()
        // An id the catalog doesn't know falls back to the comics list.
        open = "no-such-series"
        compose.waitForIdle()
        compose.onNodeWithTag("discover-list").assertIsDisplayed()
    }
}
