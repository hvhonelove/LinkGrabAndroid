# Third-party notices

LinkGrab application code: Copyright 2026 LinkGrab contributors, **GPL-3.0-only**; full terms in [LICENSE](LICENSE). This license was chosen because the included youtubedl-android integration is GPL-3.0. Do not redistribute this combined application as closed-source software.

| Component | Pinned version / source | License notes |
| --- | --- | --- |
| youtubedl-android + FFmpeg wrapper | [0.18.1](https://github.com/yausername/youtubedl-android/tree/0.18.1) | GPL-3.0; includes native runtime packages; see upstream README/build provenance |
| yt-dlp | Bundled by wrapper; optionally replaced using upstream stable updater | yt-dlp core is Unlicense; its bundled dependencies carry their respective licenses. [Source](https://github.com/yt-dlp/yt-dlp) |
| FFmpeg native runtime | Distributed by wrapper's `ffmpeg` artifact | FFmpeg licensing depends on build configuration; comply with matching upstream GPL/LGPL component obligations. [Source](https://ffmpeg.org/download.html) |
| Python / QuickJS | Distributed by wrapper | PSF / MIT respective source components and third-party notices; refer to the wrapper's matching native build provenance |
| AndroidX / Compose / Room / DataStore | Pinned in app/build.gradle.kts | Apache-2.0; [AndroidX source](https://android.googlesource.com/platform/frameworks/support/) |
| Kotlin / Coroutines | 2.1.20 / 1.10.2 | Apache-2.0 |
| Coil | 2.7.0 | Apache-2.0; [source](https://github.com/coil-kt/coil/tree/2.7.0) |
| Jackson / Commons IO | Transitive from wrapper | Apache-2.0 |
| Gradle wrapper | 8.11.1 | Apache-2.0; official wrapper from Gradle v8.11.1 tag |

Full GPL and Apache license texts are included in the app's offline license viewer. Service names are used to identify user-selected sources; LinkGrab is not affiliated with those services. The integration-test video/audio is synthesized by this project and is not included in the application APK.

Public binary redistribution must retain these notices and supply the matching corresponding source/build material for the actual bundled GPL components. Pin and record the exact upstream native binaries used for a public release; the app README documents the dependency versions, upstream source links and release-signing boundaries. An in-app yt-dlp update changes the local engine independently of the application build.
