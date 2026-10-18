package com.example.linkgrab.data.engine

import android.content.Context
import com.example.linkgrab.core.LinkParser
import com.example.linkgrab.core.DownloadFormatSelector
import com.example.linkgrab.domain.*
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class YtDlpEngine(private val context: Context) : VideoEngine {
    private val setupLock = Mutex()
    private val running = AtomicInteger()
    @Volatile private var checkedVersion: String? = null
    private suspend fun begin() = setupLock.withLock {
        withContext(Dispatchers.IO) { YoutubeDL.init(context); FFmpeg.getInstance().init(context) }
        running.incrementAndGet()
    }
    override suspend fun analyze(url: String): VideoMetadata {
        require(LinkParser.isValid(url)) { "Вставь корректную ссылку http:// или https://" }
        begin()
        val process = "metadata-${UUID.randomUUID()}"
        try {
            val request = baseRequest(url).apply {
                addOption("--dump-single-json"); addOption("--skip-download")
            }
            val raw = runInterruptible(Dispatchers.IO) { YoutubeDL.execute(request, process, false, null).out }
            val json = JSONObject(raw)
            require(!json.has("entries")) { "Это плейлист или подборка. Выбери ссылку на одно видео." }
            require(!json.optBoolean("has_drm")) { "Видео защищено DRM." }
            require(!json.optBoolean("is_live")) { "Прямые эфиры не поддерживаются. Дождись окончания трансляции." }
            val array = json.optJSONArray("formats")
            val formats = (0 until (array?.length() ?: 0)).map { array!!.getJSONObject(it) }
                .ifEmpty { listOf(json) }.filter { !it.optBoolean("has_drm") }.map {
                    MediaFormat(it.optString("format_id"), it.optInt("height"), it.optString("ext"),
                        it.optLong("filesize").takeIf { n -> n > 0 } ?: it.optLong("filesize_approx"),
                        it.optString("vcodec", "none") != "none", it.optString("acodec", "none") != "none")
                }
            return VideoMetadata(LinkParser.normalize(url), json.optString("title").ifBlank { "Видео" },
                json.optString("thumbnail").takeIf { LinkParser.isValid(it) },
                json.optDouble("duration", 0.0).toLong(), json.optString("uploader").ifBlank { json.optString("channel") },
                LinkParser.source(url), formats)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { YoutubeDL.destroyProcessById(process) }
            running.decrementAndGet()
        }
    }
    override suspend fun download(id: Long, url: String, options: DownloadOptions, folder: File,
        progress: (EngineProgress) -> Unit): File {
        begin()
        try {
            check(folder.exists() || folder.mkdirs()) { "Не удалось создать временную папку" }
            var phase = "Подготовка"
            val onProgress: (Float, Long, String) -> Unit = { percent, eta, line ->
                phase = when {
                    line.contains("[Merger]") -> "Объединение дорожек"
                    line.contains("[ExtractAudio]") -> "Преобразование аудио"
                    line.contains("[VideoRemuxer]") -> "Смена контейнера"
                    line.startsWith("[download]") -> "Скачивание"
                    else -> phase
                }
                val speed = Regex("at\\s+([^\\s]+/s)").find(line)?.groupValues?.get(1).orEmpty()
                val parsed = Regex("\\[download]\\s+(\\d+(?:\\.\\d+)?)%").find(line)?.groupValues?.get(1)?.toFloatOrNull()
                val value = if (phase == "Скачивание") parsed ?: percent else -1f
                progress(EngineProgress(value.coerceIn(-1f, 100f),
                    if (value >= 0 && value < 100) eta.coerceAtLeast(0) else 0, speed, phase))
            }
            val isTikTok = LinkParser.isTikTok(url) && options.kind == MediaKind.VIDEO
            val attempts = if (isTikTok) 2 else 1
            for (attempt in 0 until attempts) {
                val request = baseRequest(url).apply {
                    addOption("--newline")
                    addOption("--continue")
                    addOption("--no-mtime")
                    addOption("--fragment-retries", "3")
                    addOption("--extractor-retries", "3")
                    addOption("-o", File(folder, "media.%(ext)s").absolutePath)
                    if (options.kind == MediaKind.AUDIO) {
                        addOption("-f", "ba/b")
                        addOption("-x")
                        addOption("--audio-format", if (options.format == ContainerFormat.MP3) "mp3" else "m4a")
                    } else {
                        addOption("-f", DownloadFormatSelector.video(url, options.quality, alternate = attempt > 0))
                        addOption("-S", "res,ext:mp4:m4a")
                        when (options.format) {
                            ContainerFormat.MP4 -> {
                                addOption("--merge-output-format", "mp4")
                                addOption("--remux-video", "mp4")
                            }
                            ContainerFormat.WEBM -> {
                                addOption("--merge-output-format", "webm")
                                addOption("--remux-video", "webm")
                            }
                            else -> addOption("--merge-output-format", "mp4/webm/mkv")
                        }
                    }
                }
                try {
                    runInterruptible(Dispatchers.IO) {
                        YoutubeDL.execute(request, "task-$id", false, onProgress)
                    }
                    break
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (!isTikTok || attempt == attempts - 1 || !isRetryableTikTokFailure(error)) throw error
                    // A different TikTok CDN format can work even when the first stream is
                    // forbidden or temporarily unavailable. Never resume a part from another format.
                    folder.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
                    phase = "Подбор альтернативного потока TikTok"
                    progress(EngineProgress(-1f, 0, "", phase))
                }
            }
            return folder.listFiles()?.filter { it.isFile && it.length() > 0 && it.extension.lowercase() in setOf("mp4", "webm", "mkv", "m4a", "mp3", "mov", "opus") }
                ?.maxByOrNull { it.lastModified() } ?: error("Загрузчик не создал медиафайл")
        } finally { running.decrementAndGet() }
    }
    override suspend fun cancel(id: Long) { withContext(Dispatchers.IO) { YoutubeDL.destroyProcessById("task-$id") } }
    override suspend fun update(): String = setupLock.withLock {
        check(running.get() == 0) { "Дождись завершения загрузки или анализа ссылки." }
        withContext(Dispatchers.IO) {
            YoutubeDL.init(context)
            YoutubeDL.updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            checkedVersion = null
            version()
        }
    }
    override suspend fun checkVersion(): String {
        begin()
        val process = "version-${UUID.randomUUID()}"
        try {
            val request = YoutubeDLRequest(emptyList<String>()).apply { addOption("--version") }
            val value = runInterruptible(Dispatchers.IO) { YoutubeDL.execute(request, process, false, null).out.trim() }
            check(value.isNotBlank()) { "Не удалось прочитать версию загрузчика" }
            checkedVersion = value
            return value
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { YoutubeDL.destroyProcessById(process) }
            running.decrementAndGet()
        }
    }
    override fun version() = checkedVersion ?: YoutubeDL.versionName(context) ?: YoutubeDL.version(context) ?: "Встроенная версия"
    private fun isRetryableTikTokFailure(error: Exception): Boolean {
        val raw = error.message.orEmpty().lowercase()
        return listOf("403", "404", "429", "timed out", "timeout", "connection",
            "unable to download", "requested format", "no video formats", "http error")
            .any { it in raw }
    }

    private fun baseRequest(url: String) = YoutubeDLRequest(LinkParser.normalize(url)).apply {
        addOption("--no-playlist"); addOption("--socket-timeout", "20"); addOption("--retries", "2")
    }
}
