package net.blazarforce.folio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.blazarforce.folio.data.AppTheme
import net.blazarforce.folio.ui.common.FolioMotion
import net.blazarforce.folio.ui.library.LibraryScreen
import net.blazarforce.folio.ui.theme.FolioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Library's info button opens About & Privacy, which links to the hosted policy. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AboutPrivacyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun infoButtonOpensDialogAndPolicyLinkFires() {
        var policyTaps = 0
        compose.setContent {
            FolioTheme(AppTheme.INK, motion = FolioMotion(ambient = false)) {
                LibraryScreen(emptyList(), AppTheme.INK, false, {}, {}, {}, {}, {}, {}, onPrivacyPolicy = { policyTaps++ })
            }
        }
        compose.onNodeWithTag("about-privacy").assertDoesNotExist()
        compose.onNodeWithContentDescription("About & privacy").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("about-privacy").assertIsDisplayed()
        compose.onNodeWithText("Blazar Reader").assertExists()
        compose.onNodeWithText("Version ${BuildConfig.VERSION_NAME}").assertExists()
        compose.onNodeWithText("No ads, no analytics, no trackers.").assertExists()
        compose.onNodeWithText("Privacy Policy").performScrollTo().performClick()
        assertEquals(1, policyTaps)
        compose.onNodeWithText("Close").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("about-privacy").assertDoesNotExist()
    }

    @Test fun policyUrlIsTheHostedPage() {
        assertEquals("https://blazarforce.net/blazar-reader-privacy.html", MainActivity.PRIVACY_POLICY_URL)
        assertEquals("1.5", BuildConfig.VERSION_NAME)
        assertEquals(6, BuildConfig.VERSION_CODE)
    }
}
