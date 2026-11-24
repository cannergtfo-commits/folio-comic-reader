package net.blazarforce.folio

import net.blazarforce.folio.data.AppRelease
import net.blazarforce.folio.data.UpdateGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Play flavor must never surface or trigger an external APK update.
 * The Direct (BlazarSwap / GitHub) flavor shows the banner only for a newer, non-blank APK.
 */
class UpdateGateTest {
    private val newer = AppRelease("1.6", 7, "https://example.com/BlazarReader-1.6.apk", "notes")
    private val same = AppRelease("1.5", 6, "https://example.com/BlazarReader-1.5.apk", null)
    private val older = AppRelease("1.4", 5, "https://example.com/BlazarReader-1.4.apk", null)
    private val blankApk = AppRelease("1.6", 7, "  ", "notes")
    private val emptyApk = AppRelease("1.6", 7, "", "notes")

    @Test
    fun disabledGateHidesEveryRelease() {
        assertNull(UpdateGate.bannerFor(newer, currentVersionCode = 6, externalUpdatesEnabled = false))
        assertNull(UpdateGate.bannerFor(same, currentVersionCode = 6, externalUpdatesEnabled = false))
        assertNull(UpdateGate.bannerFor(null, currentVersionCode = 6, externalUpdatesEnabled = false))
    }

    @Test
    fun enabledGateShowsOnlyANewerReleaseWithAnApk() {
        val shown = UpdateGate.bannerFor(newer, currentVersionCode = 6, externalUpdatesEnabled = true)
        assertNotNull(shown)
        assertEquals("1.6", shown!!.version)
        assertEquals(7, shown.versionCode)

        assertNull(UpdateGate.bannerFor(same, currentVersionCode = 6, externalUpdatesEnabled = true))
        assertNull(UpdateGate.bannerFor(older, currentVersionCode = 6, externalUpdatesEnabled = true))
        assertNull(UpdateGate.bannerFor(null, currentVersionCode = 6, externalUpdatesEnabled = true))
        assertNull(UpdateGate.bannerFor(blankApk, currentVersionCode = 6, externalUpdatesEnabled = true))
        assertNull(UpdateGate.bannerFor(emptyApk, currentVersionCode = 6, externalUpdatesEnabled = true))
    }

    @Test
    fun compiledFlagMatchesTheDistributionFlavor() {
        val direct = BuildConfig.FLAVOR == "direct"
        assertEquals(direct, UpdateGate.enabled)
        assertEquals(direct, BuildConfig.ENABLE_EXTERNAL_UPDATE_BANNER)
    }
}
