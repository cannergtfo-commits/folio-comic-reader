package net.blazarforce.folio

import androidx.test.core.app.ApplicationProvider
import net.blazarforce.folio.data.PageTurn
import net.blazarforce.folio.data.Prefs
import net.blazarforce.folio.ui.common.FolioMotion
import net.blazarforce.folio.ui.reader.effectivePageTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MotionTest {
    @Test
    fun everyPageTurnPersists() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (t in PageTurn.values()) {
            Prefs(ctx).update { it.copy(pageTurn = t) }
            assertEquals(t, Prefs(ctx).settings.value.pageTurn)
        }
    }

    @Test
    fun reduceMotionFallsBackToFade() {
        assertEquals(PageTurn.FADE, effectivePageTurn(PageTurn.BOOK, true))
        assertEquals(PageTurn.FADE, effectivePageTurn(PageTurn.DEPTH, true))
        assertEquals(PageTurn.SLIDE, effectivePageTurn(PageTurn.SLIDE, true))
        assertEquals(PageTurn.NONE, effectivePageTurn(PageTurn.NONE, true))
        assertEquals(PageTurn.BOOK, effectivePageTurn(PageTurn.BOOK, false))
    }

    @Test
    fun loopsOnlyWhenAmbientAndNotReduced() {
        assertTrue(FolioMotion().loops)
        assertFalse(FolioMotion(reduce = true).loops)
        assertFalse(FolioMotion(ambient = false).loops)
    }
}
