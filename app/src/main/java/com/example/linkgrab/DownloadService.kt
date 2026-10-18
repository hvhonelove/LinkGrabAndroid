package com.example.linkgrab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.Locale

class DownloadService : Service() {
    companion object {
        const val ACTION_DOWNLOAD = "com.example.linkgrab.DOWNLOAD"
        const val ACTION_UPDATE = "com.example.linkgrab.UPDATE"
        const val ACTION_CANCEL = "com.example.linkgrab.CANCEL"
        const val ACTION_STATUS = "com.example.linkgrab.STATUS"
        const val EXTRA_URL = "url"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_STATUS = "status"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_BUSY = "busy"
        private const val CHANNEL = "linkgrab_downloads"
        private const val FOREGROUND_ID = 101
        private const val PROCESS_ID = "linkgrab-process"
        @Volatile var active = false
            private set
        @Volatile var latestStatus = ""
            private set
        @Volatile var latestProgress = -1
            private set
    }

    @Volatile private var cancelRequested = false
    private var lastUpdate = 0L
    private val notifications by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Загрузка видео", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelRequested = true
                if (active) {
                    showStatus("Отмена…", latestProgress)
                    runCatching { YoutubeDL.getInstance().destroyProcessById(PROCESS_ID) }
                } else stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DOWNLOAD, ACTION_UPDATE -> {
                if (active) return START_NOT_STICKY
                active = true
                cancelRequested = false
                startForeground(FOREGROUND_ID, notification("Подготовка…", -1))
                val action = intent.action
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                val quality = intent.getIntExtra(EXTRA_QUALITY, 0)
                Thread({
                    try {
                        if (action == ACTION_UPDATE) updateEngine() else download(url, quality)
                    } catch (e: Exception) {
                        val result = if (cancelRequested) "Загрузка отменена"
                        else "Ошибка: " + (e.message ?: e.javaClass.simpleName).takeLast(450)
                        showStatus(result, -1, false)
                    } finally {
                        active = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }, "LinkGrab-Worker").start()
                return START_NOT_STICKY
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun initEngine() {
        showStatus("Запуск yt-dlp и FFmpeg…", -1)
        YoutubeDL.getInstance().init(applicationContext)
        FFmpeg.getInstance().init(applicationContext)
        if (cancelRequested) throw InterruptedException("Отменено")
    }

    private fun updateEngine() {
        initEngine()
        showStatus("Обновление yt-dlp…", -1)
        val result = YoutubeDL.getInstance().updateYoutubeDL(
            applicationContext, YoutubeDL.UpdateChannel.STABLE
        )
        showStatus("yt-dlp обновлён: " + result, 100, false)
        finishedNotification("yt-dlp обновлён", null)
    }

    private fun download(url: String, selectedQuality: Int) {
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Укажи ссылку на видео"
        }
        initEngine()
        val root = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val work = File(root, "work_" + System.currentTimeMillis())
        check(work.mkdirs()) { "Не удалось создать папку для загрузки" }
        try {
            val format = when (selectedQuality) {
                1 -> "bv*[height<=1080]+ba/b[height<=1080]"
                2 -> "bv*[height<=720]+ba/b[height<=720]"
                else -> "bv*+ba/b"
            }
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--newline")
                addOption("--no-mtime")
                addOption("-f", format)
                addOption("--merge-output-format", "mp4")
                addOption("--remux-video", "mp4")
                addOption("-o", File(work, "%(title).110B [%(id)s].%(ext)s").absolutePath)
            }
            showStatus("Получаем видео…", -1)
            YoutubeDL.getInstance().execute(request, PROCESS_ID, false) { percentage, eta, _ ->
                if (!cancelRequested) {
                    val time = System.currentTimeMillis()
                    val value = percentage.toInt().coerceIn(0, 100)
                    if (time - lastUpdate > 700 || value >= 100) {
                        lastUpdate = time
                        val remaining = if (eta > 0) " · осталось " + eta + " с" else ""
                        showStatus("Загрузка: " + value + "%" + remaining, value)
                    }
                }
            }
            if (cancelRequested) throw InterruptedException("Отменено")
            showStatus("Сохраняем видео…", -1)
            val video = work.listFiles()?.filter {
                it.isFile && it.extension.lowercase(Locale.ROOT) in
                    setOf("mp4", "webm", "mkv", "mov")
            }?.maxByOrNull { it.length() } ?: error("Видео не найдено")
            val saved = saveToGallery(video)
            showStatus("Сохранено: Фильмы/LinkGrab/" + video.name, 100, false)
            finishedNotification("Видео сохранено", saved)
        } finally {
            work.deleteRecursively()
        }
    }

    private fun saveToGallery(file: File): Uri {
        val mime = when (file.extension.lowercase(Locale.ROOT)) {
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            else -> "video/mp4"
        }
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/LinkGrab")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val target = resolver.insert(
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values
        ) ?: error("Не удалось создать видео в медиатеке")
        try {
            file.inputStream().buffered().use { input ->
                val output = resolver.openOutputStream(target, "w")
                    ?: error("Не удалось открыть файл")
                output.buffered().use { input.copyTo(it) }
            }
            resolver.update(target, ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }, null, null)
            return target
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            throw e
        }
    }

    private fun showStatus(message: String, percentage: Int, busy: Boolean = active) {
        latestStatus = message
        latestProgress = percentage
        sendBroadcast(Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS, message)
            putExtra(EXTRA_PROGRESS, percentage)
            putExtra(EXTRA_BUSY, busy)
        })
        if (busy) notifications.notify(FOREGROUND_ID, notification(message, percentage))
    }

    private fun notification(message: String, percentage: Int): Notification {
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancel = PendingIntent.getService(
            this, 2, Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("LinkGrab")
            .setContentText(message)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percentage.coerceAtLeast(0), percentage < 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отмена", cancel)
            .build()
    }

    private fun finishedNotification(message: String, video: Uri?) {
        val intent = if (video == null) Intent(this, MainActivity::class.java)
        else Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(video, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val open = PendingIntent.getActivity(
            this, 3, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        notifications.notify(102, Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("LinkGrab")
            .setContentText(message)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        )
    }
}
