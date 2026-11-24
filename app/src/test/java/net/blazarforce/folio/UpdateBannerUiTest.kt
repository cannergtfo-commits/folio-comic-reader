package net.blazarforce.folio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import net.blazarforce.folio.data.AppRelease
import net.blazarforce.folio.data.AppTheme
import net.blazarforce.folio.data.LibraryState
import net.blazarforce.folio.ui.common.FolioMotion
import net.blazarforce.folio.ui.discover.DiscoverScreen
import net.blazarforce.folio.ui.theme.FolioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Discover renders the external APK update card only in the `direct` flavor, never in `play`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class UpdateBannerUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun discoverShowsUpdateCardOnlyWhenFlavorAllows() {
        val newer = AppRelease("9.9", 999, "https://example.invalid/BlazarReader-9.9.apk", "Shiny new things")
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = FolioMotion(ambient = false)) {
                // Even if a caller hands Discover a release, the play build must not render it.
                DiscoverScreen(LibraryState(error = "offline-empty"), emptySet(), newer, {}, {}, {}, {}, {})
            }
        }
        val card = compose.onNodeWithText("Blazar Reader 9.9 is out")
        if (BuildConfig.FLAVOR == "direct") card.assertExists() else card.assertDoesNotExist()
    }
}
