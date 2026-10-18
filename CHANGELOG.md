## 2.0.3 — First-use auto-update and self-repairing downloads

- Enable opt-out automatic yt-dlp update checks by default, including on existing installs that have never changed this preference.
- Check for engine updates before the first video link analysis; keep the bundled extractor available when offline or GitHub is unavailable.
- Cache successful checks for 24 hours and failures for 1 hour to avoid hitting update servers repeatedly.
- On extractor-related errors, attempt one refresh-and-retry where due; never retry indefinitely or update for local storage/FFmpeg failures.
- Show readable update status in link analysis and download progress; manual update shares the same coordination lock.
- Add JVM tests for update cadence and failure categories.

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
