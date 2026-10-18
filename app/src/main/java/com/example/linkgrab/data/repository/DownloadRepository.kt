package com.example.linkgrab.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.linkgrab.core.safeFileName
import com.example.linkgrab.data.db.*
import com.example.linkgrab.data.settings.SettingsStore
import com.example.linkgrab.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class DownloadRepository(private val context: Context, private val database: LinkGrabDatabase,
    private val storage: MediaStorage, private val engine: VideoEngine, private val settings: SettingsStore) : DownloadManager {
    val dao = database.downloads()
    val items = dao.observe()
    fun workFolder(id: Long) = File(context.filesDir, "downloads/$id")
    override suspend fun enqueue(metadata: VideoMetadata, options: DownloadOptions): Long = dao.insert(DownloadEntity(
        url = metadata.url, title = metadata.title, thumbnail = metadata.thumbnail,
        uploader = metadata.uploader, source = metadata.source, duration = metadata.duration,
        quality = options.quality, kind = options.kind, format = options.format, bytes = metadata.estimate(options)))
    override suspend fun retry(id: Long): Long = database.withTransaction {
        val item = dao.get(id) ?: error("Загрузка не найдена")
        if (item.status == TaskStatus.COMPLETED) dao.insert(item.copy(id = 0, uri = null, fileName = "",
            status = TaskStatus.QUEUED, progress = 0f, completedAt = 0, createdAt = System.currentTimeMillis(), error = null))
        else {
            check(item.status in setOf(TaskStatus.PAUSED, TaskStatus.FAILED, TaskStatus.CANCELED)) { "Загрузка уже в очереди" }
            dao.update(item.copy(status = TaskStatus.QUEUED, error = null, phase = "В очереди", speed = "", eta = 0, uri = null))
            id
        }
    }
    override suspend fun pause(id: Long) {
        dao.status(id, TaskStatus.PAUSED, "На паузе")
        engine.cancel(id)
    }
    override suspend fun cancel(id: Long) {
        dao.status(id, TaskStatus.CANCELED, "Отменено")
        engine.cancel(id)
        withContext(Dispatchers.IO) { workFolder(id).deleteRecursively() }
    }
    override suspend fun moveNext(id: Long) { dao.moveNext(id) }
    suspend fun rename(id: Long, title: String) {
        val item = dao.get(id) ?: return
        check(item.status == TaskStatus.COMPLETED && item.uri != null) { "Сначала дождись завершения загрузки" }
        val safe = safeFileName(title)
        val name = "$safe.${item.fileName.substringAfterLast('.', "mp4")}"
        storage.rename(item.uri, name)
        dao.update(item.copy(title = safe, fileName = name))
    }
    suspend fun remove(id: Long, deleteFile: Boolean) {
        val item = dao.get(id) ?: return
        if (item.status == TaskStatus.RUNNING) cancel(id)
        if (deleteFile && item.uri != null) storage.delete(item.uri)
        dao.delete(id)
        withContext(Dispatchers.IO) { workFolder(id).deleteRecursively() }
    }
    /** A committed MediaStore row wins over an interrupted database completion. */
    suspend fun recover() {
        dao.running().forEach { item ->
            if (item.uri != null && storage.isPublished(item.uri)) dao.complete(item.id, item.bytes, System.currentTimeMillis())
            else {
                item.uri?.let { runCatching { storage.delete(it) } }
                dao.update(item.copy(status = TaskStatus.PAUSED, uri = null,
                    phase = "Прервано системой — нажми «Продолжить»", speed = "", eta = 0))
            }
        }
        if (!settings.flow.first().legacyMigrated) {
            val raw = context.getSharedPreferences("history", Context.MODE_PRIVATE).getString("saved_videos", "[]")
            val legacy = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
            database.withTransaction {
                for (index in 0 until legacy.length()) {
                    val item = legacy.optJSONObject(index) ?: continue
                    val uri = item.optString("uri").takeIf { it.startsWith("content://") } ?: continue
                    dao.insert(DownloadEntity(url = "", title = item.optString("title", "Видео"), uri = uri,
                        source = item.optString("source"), bytes = item.optLong("bytes"), quality = item.optInt("quality"),
                        status = TaskStatus.COMPLETED, phase = "Сохранено", progress = 100f,
                        createdAt = item.optLong("savedAt"), completedAt = item.optLong("savedAt"), fileName = item.optString("title") + ".mp4"))
                }
            }
            settings.flag("legacyMigrated", true)
        }
    }
}
