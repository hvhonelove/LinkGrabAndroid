# Changelog

## 2.0.1

- Added explicit TikTok and VK Video source labels and home chips.
- Canonicalized TikTok Android-share, embed and short links; removed sharing/tracking parameters from TikTok video URLs.
- Added VK domain/mobile/encoded-player URL normalization while preserving access and embed parameters.
- Excluded TikTok watermarked/download variants in both metadata previews and video/audio download selectors; no silent branded fallback.
- Bundled SHA-256-verified yt-dlp 2026.08.19 and migrated older installed engines atomically without downgrading newer manual updates.
- Added platform URL regression tests and native selector tests rejecting a higher-resolution watermarked format and a watermarked-only source.
- Stabilized the rename UI regression test by waiting for the queue snackbar to disappear.

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
