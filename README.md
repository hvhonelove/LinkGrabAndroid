# LinkGrab Android

Android video downloader for public videos from X/Twitter, YouTube and other sites supported by yt-dlp.

## Version 1.1

- Dark mint interface with Home, Downloads and Settings tabs; custom vector app icon.
- Paste or Android Share input, source detection, URL validation and remembered quality (best/1080p/720p).
- Live progress, cancellation, readable errors and an optional notification permission prompt at first use.
- Local download history with video size/date/source, open and Android Share actions. Clearing history keeps gallery files.
- Videos save to **Movies/LinkGrab** through MediaStore, without broad gallery access.
- Engine update in Settings. No sample URL or fake downloads in the app.

History starts with videos saved by this version and retains the most recent 200 entries. Files removed outside the app are reported as unavailable when opened.

## APK
Go to **Actions → Build Android APK**. Open the latest successful run (both APK/lint and emulator UI checks should pass), then download its **LinkGrab-android-arm64-apk** artifact, unzip, and install **app-debug.apk** on an ARM64 Android phone (Android 10+).

Powered by [youtubedl-android](https://github.com/yausername/youtubedl-android) 0.18.1 (GPL-3.0). Downloads are not guaranteed for login-gated, DRM-protected or blocked videos. Use only for content you're permitted to save.

Project uses JDK 17, Gradle 8.11.1, Android Gradle Plugin 8.9.2 and SDK 35.


## UI verification

The workflow runs `assembleDebug` and `lintDebug`, plus native smoke checks on an Android 35 emulator: URL extraction and source boundaries, clipboard paste, invalid URL feedback, quality persistence, navigation, progress, history roundtrip, unavailable media and landscape recreation. Screenshots are uploaded as **LinkGrab-UI-screenshots**. Sample history is created only by instrumentation on the emulator and removed afterward.
