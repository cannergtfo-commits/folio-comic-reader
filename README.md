# Folio — a calm, editorial comic reader for Android

`Folio.apk` (package `net.blazarforce.folio`, minSdk 26 / Android 8.0, targetSdk 35). Kotlin + Jetpack Compose (Material 3).

## Download

All installs and chapter files live on the [v1.0 release](https://github.com/cannergtfo-commits/folio-comic-reader/releases/tag/v1.0) (not in the git tree).

* [Folio.apk](https://github.com/cannergtfo-commits/folio-comic-reader/releases/download/v1.0/Folio.apk) — open this on your Android phone, then allow installing from the browser if asked.
* Lyra B&W chapters (CBZ for Folio, PDF as a fallback), also on that release: Ch1–Ch6.

## Install
After downloading `Folio.apk`, open it on the phone (allow "install unknown apps" for your browser or file manager), or
`adb install -r Folio.apk`.

## Use
* **Import**: `+` → *Comic files…* (CBZ / PDF, multi-select) or *A folder…* (finds CBZ/PDF inside, and turns folders
  of images into books). Or tap a .cbz/.pdf in any file manager and choose **Open with Folio**.
* CBZ/PDF files are **copied into app storage** (so they keep working after the original moves). Image folders are
  read in place via a persisted folder permission.
* Library: Continue-reading hero, series stacks (from ComicInfo `Series`/`Number`, or names like `Lyra_Ch1`),
  filters (All / Reading / Unread / Finished), long-press a book for details, mark read/unread, remove.
* Reader: tap the left / right third to turn pages, the centre to show controls; swipe; pinch or double-tap to zoom.
  Controls: page slider with live thumbnail preview, thumbnail strip, RTL/LTR pill, and ⚙ for layout
  (single / two-page spread / vertical scroll), page turn (slide / fade / instant), brightness, warmth filter, haptics.
  Landscape shows two-page spreads automatically (toggle in settings). Position is saved per book; the last page
  offers "Up next" in the series.

## Build (on the box)
```
source env.sh            # JDK 17 + Gradle 8.14.3 in /home/box/tools, SDK in /home/box/android-sdk
./gradlew :app:assembleRelease        # → app/build/outputs/apk/release/app-release.apk
./gradlew :app:recordRoborazziDebug   # unit tests + screenshots → screenshots/
```
Release signing uses `keystore/folio-release.jks` with credentials in `keystore/keystore.properties`
(local only — keep both safe; you need the same key to ship updates).

Fonts: Cormorant Garamond and Inter (SIL Open Font License), instanced/subset to Latin.
