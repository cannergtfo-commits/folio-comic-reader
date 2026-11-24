package net.blazarforce.folio.data

import net.blazarforce.folio.BuildConfig

/**
 * Decides whether the in-app "new version" banner (which links to an APK on GitHub) may appear.
 *
 * The `direct` flavor (BlazarSwap / GitHub sideload) enables it. The `play` flavor compiles it out,
 * because Google Play builds must update only through Play (Device and Network Abuse policy).
 */
object UpdateGate {
    /** True when this build is allowed to show or open an external APK update link. */
    val enabled: Boolean get() = BuildConfig.ENABLE_EXTERNAL_UPDATE_BANNER

    /** Returns the release to advertise, or null when the banner must stay hidden. */
    fun bannerFor(
        release: AppRelease?,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        externalUpdatesEnabled: Boolean = enabled,
    ): AppRelease? {
        if (!externalUpdatesEnabled || release == null) return null
        return release.takeIf { it.versionCode > currentVersionCode && it.apk.isNotBlank() }
    }
}
