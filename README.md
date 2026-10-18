# LinkGrab 2

A free, local-first Android video/audio downloader with a violet Compose interface. No account, ads, paywall, analytics, or custom backend. Android 10+, ARM64 by default. Light, dark, and system themes. Application source is licensed **GPL-3.0-only**.

> Download only content you own or have permission to save. Some services may restrict downloading. DRM-protected, private, or login-restricted content may not work.

## Features

- First-launch onboarding and native splash screen.
- Paste a link, receive Android Share text, or opt in to a clipboard suggestion on resume.
- Analyze links with the actual bundled yt-dlp executable: title, source, uploader, thumbnail, duration, available standard resolutions and size estimates when supplied by the source.
- Video: best, 1080p, 720p, 480p when listed by the source; automatic MP4-preferred container or explicit MP4/WebM. Audio: M4A/MP3 through FFmpeg.
- Persistent sequential queue with foreground progress, speed, ETA, pause, resume, cancellation, retry and move-next priority.
- Room history: active/complete/failed-and-canceled filters, search, details, rename, share, open, location, redownload and confirmed file/history deletion.
- MediaStore destinations: `Movies/LinkGrab` and `Music/LinkGrab`. No broad storage/gallery permission.
- DataStore preferences: appearance, default resolution/type/audio format, clipboard suggestions, completion notifications, optional daily engine auto-update.
- Manual yt-dlp update and installed-version inspection.
- Recovery of interrupted jobs, pending MediaStore rollback, and migration of LinkGrab 1.1 history.

## Build

Requirements: JDK 17; Android SDK platform 36 and build tools 35.0.0. The Gradle wrapper pins **8.11.1**, AGP **8.10.1**, Kotlin/Compose compiler **2.1.20**, Compose BOM **2025.05.01**, Room **2.7.1**, and youtubedl-android **0.18.1**. These are a pinned compatible baseline rather than floating latest dependencies.

```sh
# Set ANDROID_HOME or configure sdk.dir in local.properties.
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Install on an ARM64 phone with USB debugging:
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Compile an unsigned release:
./gradlew :app:assembleRelease
```

Windows: use `gradlew.bat`. Open the repository in Android Studio as a Gradle project. For an x86_64 emulator use `-PtestAbi=x86_64`. ARM64 is the default to keep install size reasonable; other ABIs depend on the native wrapper distribution.

For public releases, configure a private release signing key and keep it across releases. The Gradle signing configuration accepts environment variables `LINKGRAB_KEYSTORE` (absolute path), `LINKGRAB_STORE_PASSWORD`, `LINKGRAB_KEY_ALIAS`, and `LINKGRAB_KEY_PASSWORD`; with all four set, `assembleRelease` signs the APK. Without them it builds an unsigned release. The downloadable CI debug APK is installable but uses a disposable debug key: an older installation signed with a different key must be removed first. Never commit a private signing key. Release minification is deliberately disabled to preserve upstream reflection and native integration.

## Structure

```text
app/src/main/java/com/example/linkgrab/
  LinkGrabApplication.kt          # manual dependency injection / startup recovery
  MainActivity.kt                 # share intents, permissions and external file actions
  core/                          # URL parsing, display/file helpers, error mapping
  domain/                        # models and engine/storage/download-manager interfaces
  data/
    db/                          # Room entity, DAO, converters, schema
    engine/                      # real yt-dlp/FFmpeg integration
    repository/                  # persistent queue commands, legacy import, recovery
    settings/                    # Preferences DataStore
    storage/                     # transactional MediaStore publication
  service/                       # foreground lifecycle, queue processor, notifications
  feature/
    home/                        # analysis ViewModel, home, onboarding
    preview/                     # metadata preview + options sheet
    downloads/                   # downloads ViewModel, list, file details and actions
    settings/                    # settings ViewModel, settings/about screens
  ui/                            # root routing, reusable controls, light/dark theme
app/src/test/                    # pure Kotlin URL/options/filename/error tests
app/src/androidTest/             # Compose + real native engine/MediaStore integration
app/schemas/                     # Room schema exported by builds
scripts/emulator-tests.sh        # emulator test execution and screenshot collection
```

The project has one Gradle application module with separated packages to keep configuration and build costs modest. UI observes ViewModel `StateFlow`; repositories own persistence, the foreground service owns work, and the engine/storage implementations run blocking I/O away from the main thread. Navigation/tab/detail state and the input URL survive configuration changes; metadata is fetched again after process death.

## Queue and recovery contract

Room is authoritative. A single service drains queued jobs in priority/FIFO order. Pause stops the native process and keeps deterministic private work files. Resume re-extracts the original URL and asks yt-dlp to continue partial files. Cancel removes private work files. Failures retain temporary files for retry. The UI never invents a completed job or successful pause.

Saving creates a pending MediaStore row, records its URI in Room, copies the finished file, publishes it, then commits completion. On next process startup, an already-published row is recovered as complete; incomplete publication is rolled back and the job becomes paused. Queued tasks remain in Room. Interrupted work is **not automatically started from the background or after boot**; open the app and resume/start the queue. Android force-stop prevents execution until a user opens the app again.

## CI and testing

GitHub Actions builds debug and unsigned release APKs, runs JVM tests and lint, then runs an Android 36 x86_64 emulator. Integration tests use a tiny bundled test-only MP4 served by a local socket and execute the **real** bundled yt-dlp and FFmpeg. They cover onboarding, analysis, queue-to-MediaStore video publication, byte integrity, Compose navigation, filename changes, deletion, audio extraction, both themes and rotation. The production app contains no test server or fake media records. Screenshots and JUnit reports are uploaded as artifacts.

```sh
./gradlew -PtestAbi=x86_64 :app:connectedDebugAndroidTest
```

CI artifacts: `LinkGrab-2-android-arm64-apk`, `LinkGrab-build-reports`, `LinkGrab-2-native-test-results`.

## Honest limitations

- Site support and metadata completeness are controlled by yt-dlp and the source. No cookies/login, CAPTCHA, DRM bypass, live-stream capture, playlist downloading, or guarantee that any particular site is available.
- Pausing is stop-and-resume of partial files, not a suspended TCP session. Some sources require restarting from zero. FFmpeg postprocessing can be indeterminate; displayed speed/ETA may be absent or apply to one stream of a video/audio merge.
- Automatic mode prefers MP4 but can produce WebM or MKV. Explicit remuxing cannot make an incompatible codec compatible. Audio conversion may take additional time; estimated sizes are not guarantees.
- Queue concurrency is one. Recovery requires opening the app; no boot receiver or hidden background auto-start. Android 15+ foreground `dataSync` time limits still apply; timeout saves the interrupted task as paused. Samsung/OEM battery policies and available disk space also apply.
- Completion-notification preference cannot remove Android's required active foreground-service notification. Denying notification permission does not turn a background download into an invisible operation.
- Folder opening is delegated to the device's file manager; unsupported file managers get a readable destination path. Files deleted externally remain in history until removed; opening one explains that it is unavailable.
- Network engine updates are initiated in-app, use the upstream stable channel, and do not create a persistent updater task.
- CI validates the bundled native runtime on the emulator's page size/ABI. Additional ARM64 hardware, 16 KB page-size and store-distribution validation is required before claiming compatibility with every device.

See [ARCHITECTURE.md](ARCHITECTURE.md), [CHANGELOG.md](CHANGELOG.md), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
