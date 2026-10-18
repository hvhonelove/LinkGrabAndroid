# Architecture and state contracts

`MainActivity → Compose screens → ViewModels → DownloadRepository → Room / MediaStorage`

`HomeViewModel → VideoEngine.analyze → preview → repository.enqueue → foreground service → QueueProcessor → VideoEngine.download → MediaStorage.publish → Room completion`

## Ownership

- `AppContainer`: process-lifetime instances of Room, DataStore, native engine and repository; one startup-recovery deferred.
- `HomeViewModel`: URL validation, cancelable analysis, metadata/errors and enqueue guard.
- `DownloadsViewModel`: user commands; lifecycle-aware history flow.
- `SettingsViewModel`: persistent preferences and serialized native update feedback.
- `DownloadService`: foreground/wake-lock lifecycle and serial queue draining. Android timeout cancels its coroutine; cleanup marks unfinished native work paused.
- `QueueProcessor`: claim, progress throttling, stop monitoring, optional engine update, publication and terminal status.
- `YtDlpEngine`: initialized once under mutex; independent analysis/download process IDs; updater cannot replace the executable while operations are active.
- `MediaStoreStorage`: pending-row transactional file publication, cleanup on failure, app-owned URI rename/delete.

## Transitions

`QUEUED → RUNNING → COMPLETED | FAILED | PAUSED | CANCELED`

`PAUSED | FAILED | CANCELED → QUEUED`

Redownload of a completed file creates a new task and retains the old file/history. App/process restart turns interrupted RUNNING rows into PAUSED, unless their recorded MediaStore row was already published. Cancel/delete are explicit user actions; history-only removal preserves completed media. Renaming preserves the actual extension.

The service executes one download at a time. Room progress writes use a conflated channel so frequent native callbacks cannot create an unbounded work backlog. User stop state is rechecked while the native process runs and while the finished file is copied. Blocking native execution uses `runInterruptible(Dispatchers.IO)`; storage runs on I/O, Room dispatches queries, and UI collects Flow using lifecycle awareness.

## Persistence

Room schema version 1 is the first Room-backed release; old 1.1 SharedPreferences records are imported once with a URI uniqueness constraint. No destructive fallback migration is configured. Future schema changes must add Room migrations and retain exported schemas.

DataStore I/O errors fall back to defaults without overwriting stored preferences. Settings changes do not silently request Android permissions; notification permission is requested after the user's first download action or from Settings.

## Release boundaries

There is no analytics collection, user account or LinkGrab server. Video metadata/bytes still require network access to the user's chosen source, and thumbnails are loaded by Coil from the source URL. Engine updates contact GitHub. Release signing credentials belong outside the repository. Application package ID remains compatible with version 1.x data when APK signing identity is retained.
