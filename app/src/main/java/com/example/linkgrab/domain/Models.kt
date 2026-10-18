package com.example.linkgrab.domain

enum class TaskStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELED }
enum class MediaKind { VIDEO, AUDIO }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ContainerFormat { AUTO, MP4, WEBM, M4A, MP3 }
data class DownloadOptions(val quality: Int = 0, val kind: MediaKind = MediaKind.VIDEO,
    val format: ContainerFormat = ContainerFormat.AUTO)
data class MediaFormat(val id: String, val height: Int, val extension: String, val bytes: Long,
    val video: Boolean, val audio: Boolean)
data class VideoMetadata(val url: String, val title: String, val thumbnail: String?,
    val duration: Long, val uploader: String, val source: String, val formats: List<MediaFormat>) {
    val qualities: List<Int> get() = listOf(0) + listOf(1080, 720, 480).filter { q -> formats.any { it.video && it.height == q } }
    fun estimate(options: DownloadOptions): Long {
        val audio = formats.filter { it.audio && !it.video }.maxOfOrNull { it.bytes } ?: 0
        if (options.kind == MediaKind.AUDIO) return audio
        val video = formats.filter { it.video && (options.quality == 0 || it.height <= options.quality) }
            .maxWithOrNull(compareBy<MediaFormat> { it.height }.thenBy { it.bytes }) ?: return 0
        return if (video.bytes == 0L) 0 else video.bytes + if (video.audio) 0 else audio
    }
}
data class EngineProgress(val percent: Float, val eta: Long, val speed: String)
const val LEGAL_NOTICE = "Download only content you own or have permission to save. Some services may restrict downloading. DRM-protected, private, or login-restricted content may not work."
