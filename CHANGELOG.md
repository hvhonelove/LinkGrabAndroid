## 2.0.2 — Retry unavailable TikTok streams and expose download diagnostics

- Prefer TikTok direct play formats ahead of potentially unavailable bitrate formats.
- Retry once with an alternate non-watermarked format when the TikTok stream returns connection, HTTP or format errors.
- Show redacted technical error details on the failed-download details screen; list cards still show a short message.
- Better distinguish CDN access failures from general connectivity issues.
- Add JVM regression tests for format fallback and diagnostic redaction.

## Unreleased — TikTok and VK Видео improvements

- Recognize TikTok, TikTok short links, VK Video and vkvideo.ru URLs.
- Strip public TikTok share tracking while retaining VK access parameters.
- Prefer TikTok clean playback streams and filter out formats labeled as watermarked.
- Add home source chips, preview note, URL regression tests and format-selector tests.

# Changelog

## 2.0.0

- Replaced programmatic Android views with reusable Compose components, violet light/dark/system themes and adaptive inset handling.
- Added onboarding, native splash, link analysis, metadata preview, video/audio selectors and format sheet.
- Introduced ViewModels, repository interfaces, Room history/queue and DataStore preferences.
- Added persistent sequential foreground downloads, pause/resume, move-next, cancel/retry, progress/speed/ETA and interruption recovery.
- Added MediaStore audio/video publication, rename/share/open/location/delete, history search/filters and migration from 1.1.
- Added optional clipboard suggestions, engine-update preferences, about/legal/license screens, and public build/architecture documentation.
- Added wrapper, JVM checks and on-device real yt-dlp/FFmpeg/MediaStore + Compose integration tests.

## 1.1.0

Mint native interface, local JSON history and simple foreground video downloads.
