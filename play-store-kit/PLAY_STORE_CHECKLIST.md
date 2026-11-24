# Blazar Reader 1.5 — Google Play listing kit

Prepared locally on the `play-store-prep` branch. Nothing in this kit has been uploaded to Google Play, GitHub, or BlazarSwap.

**App name:** Blazar Reader
**Length:** 13 / 30 characters

**Package name:** `net.blazarforce.blazarreader`
**Version:** 1.5 (`versionCode` 6)
**Default language:** English (United States)
**Category:** Tools (or Books & Reference — Tools matches the BlazarSwap shelf)
**Website:** https://blazarforce.net/blazar-apps.html#blazar-reader
**Privacy policy:** https://blazarforce.net/blazar-reader-privacy.html
  (Live on blazarforce.net; the same file is `blazar-reader-privacy.html` in this folder.)
**Support email:** canner.gtfo@gmail.com (on the privacy page; enter it as the Play Console store listing contact email).

## Builds (verified locally, not uploaded)

| Artifact | Path | Size | SHA-256 |
|---|---|---|---|
| Play App Bundle | `/workspace/comic-reader-app/build-out/BlazarReader-1.5-play.aab` | 3,413,295 bytes | `03d4d372b6191e419dd946eab1d5d203dd0f1a5d7cebea45bc360e936ba263bb` |
| Direct APK | `/workspace/comic-reader-app/build-out/BlazarReader-1.5-direct.apk` | 1,606,763 bytes | `d2e0638e09b162fa64182b0e7aa40876a2b0e201909f6effdcfb9861b3c8ad18` |

Both are signed with the existing release certificate (`CN=Folio, O=Blazar Force, C=US`, SHA-256 `e11c9494f3322b7bc8c4e1eef8a5feb902632b90833156f7329704de8141750f`). `aapt2 dump badging` on the direct APK: package `net.blazarforce.blazarreader`, `versionCode` 6, `versionName` 1.5, `minSdkVersion` 26, `targetSdkVersion` 36, label `Blazar Reader`. The Play AAB packaged manifest matches those values. Neither release manifest includes the test-only `ComponentActivity` or `android:debuggable`. `BuildConfig.ENABLE_EXTERNAL_UPDATE_BANNER` is `false` in `playRelease` and `true` in `directRelease`.

**16 KB page-size:** every `.so` in the `.aab` is `libandroidx.graphics.path.so` (arm64-v8a, armeabi-v7a, x86, x86_64). Every `PT_LOAD` segment, including both 64-bit ABIs, has `Align` `0x4000` (16384). No dependency bump was required.

## Store graphics (this folder)

| Asset | File | Spec |
|---|---|---|
| High-res icon | `icon-512-rgba.png` | 512×512, 32-bit RGBA PNG, under 1 MB |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500, 24-bit RGB PNG, no alpha |
| Phone screenshot 1 | `screenshots/01_discover_comics.png` | 1080×1920, 24-bit RGB, Discover comics list (Ink) |
| Phone screenshot 2 | `screenshots/02_library_shelf.png` | 1080×1920, 24-bit RGB, Library shelf (Ink) |
| Phone screenshot 3 | `screenshots/03_series_chapters.png` | 1080×1920, 24-bit RGB, series chapter list (Ink) |
| Phone screenshot 4 | `screenshots/04_reader_settings.png` | 1080×1920, 24-bit RGB, reader with settings |
| Phone screenshot 5 | `screenshots/05_about_privacy.png` | 1080×1920, 24-bit RGB, About & Privacy dialog |

Play requires at least two phone screenshots. Do not upload the raw Robolectric frames in `raw-shots/`.

## Short description (75 / 80 characters)

```
An editorial CBZ and PDF reader with a private shelf and an online library.
```

## Full description (1352 / 4000 characters)

```
Blazar Reader is an editorial comic reader for Android. Bring in your own CBZ and PDF files, or browse the online library and download new chapters as they are published — no app update required.

A quiet shelf
• Continue where you left off, with progress kept for every book
• Series are grouped on the shelf; standalones keep their own place
• Import comic files, a folder of page images, or open a CBZ or PDF from another app
• Ink and Paper themes, considered typography, and an ink-and-gold design

A considered reader
• Page turns: slide, book-style flip, depth, fade, or instant
• Optional two-page spreads in landscape
• Vertical-scroll (webtoon) mode
• Warmth and brightness controls for late-night reading
• Right-to-left support for manga and other RTL comics

Discover
• Browse comics, then open a title to see its chapters
• Get one chapter or the whole series
• New chapters appear in Discover when they are published
• After one successful load, the catalog stays available offline

Privacy
Blazar Reader does not create an account, show ads, or include analytics. Your library and reading progress stay on this device. The online catalog and chapter files are fetched over HTTPS. Read the full policy at https://blazarforce.net/blazar-reader-privacy.html, or from About & Privacy in the app.

Blazar Reader is published by Blazar Force.
```

## Data Safety form

Answer **No** to “Does your app collect or share any of the required user data types?”

The app does not collect or share personal information. There is no account, no analytics, no advertising, and no crash SDK. Library, reading progress, and reader preferences stay in the app’s private storage on the device.

Optional follow-ups, if Console still shows them:

- **Collected / shared data types:** none.
- **Encryption in transit:** yes, for the public catalog and chapter downloads (HTTPS to GitHub).
- **Users can request deletion:** not applicable (no server-side account). Uninstalling the app removes the on-device library.
- **Independent security review:** no.
- **Family Policy / Designed for Families:** no (do not enroll).
- **Android Auto Backup:** enabled in the manifest (`allowBackup="true"`). If the user has Backup turned on, Android may include the on-device library in that device’s Google backup. Blazar Force does not receive those backups. This is disclosed in the Privacy Policy.

## Ads

Declare **No, my app does not contain ads.**

## App access

All features are available without an account or login. Do not claim restricted content or attach login instructions.

## Target audience and content

- **Target age groups:** 13–15, 16–17, and 18 and over. Leave under-13 **unchecked** so the Families Policy does not apply.
- The app is a general-audience reader. It is not directed at children.
- The in-app catalog currently includes independently published comics (Lyra, Dead Letter Riders, Undirheim). Review those titles before you submit, and complete the IARC questionnaire honestly.

## IARC content rating

Complete the official questionnaire in Play Console rather than guessing a rating. Suggested facts for the form:

- This is a utility / media-reader app, not a game with its own story mode.
- The app UI has no violence, sexual content, profanity, or simulated gambling.
- Users can import their own comic files, which the app does not moderate.
- Discover can present third-party comics hosted in the public catalog. Rate the app for the catalog you actually ship, not only the chrome.
- No user-generated content is published through the app (readers cannot post or chat).
- No ads, no location sharing, no digital goods in this build.

## Play App Signing (do this before the first open or production release)

Blazar Reader 1.4 already uses package ID `net.blazarforce.blazarreader` and is signed with the existing release certificate (public SHA-256 starting `e11c9494` and ending `750f` — the same key used for earlier Folio / Blazar Reader builds).

If you want people who sideloaded Blazar Reader 1.4 from BlazarSwap or GitHub to be able to update in place from Google Play, **enroll that existing key as the app signing key** when Play Console first offers Play App Signing. Do this before the first open-testing or production upload. If Google is allowed to generate a new app signing key instead, Play builds will not install over the sideloaded 1.4/1.5 direct APK; those users would have to uninstall first.

Upload the **play** `.aab` (`BlazarReader-1.5-play.aab`), not the direct APK. Keep the release keystore and its password private; they are not in this kit and must not be committed.

The `play` flavor compiles out the in-app banner that links to a GitHub APK, which is required by Google Play’s Device and Network Abuse policy (apps on Play must not send users to an external APK to update).

## Walkthrough — what you do in Play Console

1. Open [Google Play Console](https://play.google.com/console) and register as a developer if you have not already. This is a one-time **$25** fee, plus government ID and contact verification. This kit does not register or pay that fee for you.
   - A **Personal** account created after November 2023 must run a closed test with at least [12 testers opted in continuously for 14 days](https://support.google.com/googleplay/android-developer/answer/14151465) before you can apply for production.
   - An **Organization** account needs a free D-U-N-S number and is exempt from that 14-day closed-test gate.
   - Personal accounts also require verification on a real Android phone.
2. Create the app: name **Blazar Reader**, default language English, type **App**, free, category Tools (or Books & Reference), and declare the Privacy Policy URL `https://blazarforce.net/blazar-reader-privacy.html` (live).
3. Finish the declarations above: Data Safety, Ads, App access, target audience, IARC, News apps (no), COVID-19 (no), Data safety, Financial features (no), etc.
4. Set up Play App Signing as described above, then upload `BlazarReader-1.5-play.aab` to a **closed testing** track first (required for a personal account; still a good idea for an organization account).
5. Add the icon, feature graphic, short and full descriptions, and the five phone screenshots.
6. Invite at least 12 testers (personal account), have them opt in and stay opted in for 14 days, then apply for production review.

## Still needed from you

1. Your **Play Console account type** (Personal vs Organization) and, when you are ready, confirmation to enroll the existing signing key.
2. The $25 Play developer registration, which only you can pay.

Done: support email (canner.gtfo@gmail.com) is on the live privacy page, and Dead Letter Riders is credited to **Blazar Force** in the catalog and in the Play screenshots and feature graphic.

The privacy policy URL is live over HTTPS, so the `.aab` is ready to upload.

## Regenerating the graphics (after the Dead Letter Riders author swap, or any UI change)

1. Record fresh frames from the play build:
   `source env.sh && FOLIO_SHOTS=/workspace/play-shots ./gradlew --offline :app:recordRoborazziPlayDebug --tests 'net.blazarforce.folio.ScreenshotTest'`
2. Rebuild the icon, feature graphic, and the five 1080×1920 screenshots:
   `/workspace/play-kit-venv/bin/python tools/play_kit.py` (needs `cairosvg` and `pillow`)

The feature graphic uses the real Discover (front) and Library (back) screens in framed phone mockups on Ink `#0E0D0B` with a warm gold glow, the gold page emblem, a Cormorant "BLAZAR READER" wordmark, and the subtitle "Editorial Comic Reader · Offline & Online Library". All content sits at least 50 px inside the edges.

## Manifest note

Besides `android.permission.INTERNET`, the merged manifest lists `net.blazarforce.blazarreader.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. AndroidX Core adds this automatically. It is a signature-level permission private to this app, not a user-facing permission, and Play does not ask you to declare it.
