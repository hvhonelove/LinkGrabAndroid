package com.example.linkgrab.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.linkgrab.core.safeFileName
import com.example.linkgrab.data.db.DownloadEntity
import com.example.linkgrab.domain.*
import kotlinx.coroutines.*
import java.io.File

class MediaStoreStorage(context: Context) : MediaStorage {
    private val resolver = context.contentResolver
    override suspend fun publish(task: DownloadEntity, file: File,
        created: suspend (String, String, String) -> Unit, stillRunning: suspend () -> Boolean): PublishedMedia = withContext(Dispatchers.IO) {
        val ext = file.extension.lowercase()
        val mime = when (ext) {
            "m4a" -> "audio/mp4"; "mp3" -> "audio/mpeg"; "opus" -> "audio/ogg"
            "webm" -> "video/webm"; "mkv" -> "video/x-matroska"; "mov" -> "video/quicktime"
            else -> "video/mp4"
        }
        val name = safeFileName(task.title) + ".$ext"
        val audio = task.kind == MediaKind.AUDIO
        val folder = if (audio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val collection = if (audio) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/LinkGrab")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }) ?: error("Не удалось создать файл в медиатеке")
        try {
            created(uri.toString(), name, mime)
            file.inputStream().buffered().use { input ->
                (resolver.openOutputStream(uri, "w") ?: error("Файл недоступен для записи")).buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var blocks = 0
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        if (blocks++ % 4 == 0 && !stillRunning()) throw CancellationException("Операция остановлена")
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (!stillRunning()) throw CancellationException("Операция остановлена")
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) > 0)
            PublishedMedia(uri.toString(), name, mime, file.length())
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }
    override suspend fun isPublished(uri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            resolver.query(Uri.parse(uri), arrayOf(MediaStore.MediaColumns.IS_PENDING, MediaStore.MediaColumns.SIZE), null, null, null)?.use {
                it.moveToFirst() && it.getInt(0) == 0 && it.getLong(1) > 0
            } ?: false
        }.getOrDefault(false)
    }
    override suspend fun size(uri: String): Long = withContext(Dispatchers.IO) {
        resolver.query(Uri.parse(uri), arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        } ?: 0L
    }
    override suspend fun delete(uri: String) { withContext(Dispatchers.IO) { resolver.delete(Uri.parse(uri), null, null) } }
    override suspend fun rename(uri: String, name: String) { withContext(Dispatchers.IO) {
        check(resolver.update(Uri.parse(uri), ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name) }, null, null) > 0) {
            "Файл удалён или недоступен"
        }
    } }
}
