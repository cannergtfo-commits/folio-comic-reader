# Blazar Reader — a calm, editorial comic reader for Android

`BlazarReader-1.4.apk` (package `net.blazarforce.blazarreader`, minSdk 26 / Android 8.0, targetSdk 35). Kotlin + Jetpack Compose (Material 3). The launcher name is **Blazar Reader**. This is a new application id, so it installs alongside Folio 1.1–1.3 (`net.blazarforce.folio`) rather than replacing them; uninstall Folio after switching if you no longer need the old shelf.

## Download

* **[Blazar Reader 1.4 APK](https://github.com/cannergtfo-commits/folio-comic-reader/releases/download/v1.4/BlazarReader-1.4.apk)** — built on the `rename-blazar-reader` branch; attach this file to a `v1.4` GitHub release before this link goes live. Also listed on [Blazar Apps](https://blazarforce.net/blazar-apps.html) under Tools (the live shelf still shows Folio 1.3 until the store listing is updated).
* Earlier builds, still installable: [Folio 1.3](https://github.com/cannergtfo-commits/folio-comic-reader/releases/download/v1.3/Folio.apk), [Folio 1.1](https://github.com/cannergtfo-commits/folio-comic-reader/releases/download/v1.1/Folio.apk).
* Chapters come from the in-app **Discover** tab (online library). Chapter files: [`library` release](https://github.com/cannergtfo-commits/folio-comic-reader/releases/tag/library). Older 1.0 downloads, with PDFs: [v1.0](https://github.com/cannergtfo-commits/folio-comic-reader/releases/tag/v1.0).

## Online library

Blazar Reader fetches [`catalog/catalog.json`](catalog/catalog.json) on launch and on pull-to-refresh, keeps a copy for
offline browsing, and downloads a chapter onto the shelf with one tap. New chapters show a **NEW** badge.
The catalog URL is unchanged (`https://raw.githubusercontent.com/cannergtfo-commits/folio-comic-reader/main/catalog/catalog.json`) so existing 1.1–1.3 installs keep working.
To publish a chapter or a new comic, see [PUBLISHING.md](PUBLISHING.md) (`tools/publish_chapter.py`).

## Install
After downloading `BlazarReader-1.4.apk`, open it on the phone (allow "install unknown apps" for your browser or file manager), or
`adb install -r BlazarReader-1.4.apk`. This is a new package (`net.blazarforce.blazarreader`), so it will not replace a Folio 1.1–1.3 install or inherit its shelf. You can keep both, or uninstall Folio after you have the chapters you want in Blazar Reader.

## Use
* **Discover**: the bottom switch flips between your Shelf and the online library. Tap **Get** on a chapter
  (or **Get all** on a series); it lands on the shelf and reads offline.
* **Import**: `+` → *Comic files…* (CBZ / PDF, multi-select) or *A folder…* (finds CBZ/PDF inside, and turns folders
  of images into books). Or tap a .cbz/.pdf in any file manager and choose **Open with Blazar Reader**.
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
./gradlew :app:bundleRelease          # → app/build/outputs/bundle/release/app-release.aab (Google Play)
./gradlew :app:recordRoborazziDebug   # unit tests + screenshots → screenshots/
```
Release signing uses `keystore/folio-release.jks` with credentials in `keystore/keystore.properties`
(local only — keep both safe; you need the same key to ship updates). The keystore filename and alias are internal.

Icon: `tools/icon/gen.py` writes the adaptive icon layers and `tools/icon/folio-icon-512.png` (store icon). The mark is a gold comic page (no letter F, no word "Folio").

Fonts: Cormorant Garamond and Inter (SIL Open Font License), instanced/subset to Latin.
