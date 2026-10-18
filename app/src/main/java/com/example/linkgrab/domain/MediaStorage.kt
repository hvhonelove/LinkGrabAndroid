package com.example.linkgrab.domain

import com.example.linkgrab.data.db.DownloadEntity
import java.io.File

data class PublishedMedia(val uri: String, val name: String, val mime: String, val bytes: Long)
interface MediaStorage {
    suspend fun publish(task: DownloadEntity, file: File,
        created: suspend (String, String, String) -> Unit, stillRunning: suspend () -> Boolean): PublishedMedia
    suspend fun isPublished(uri: String): Boolean
    suspend fun size(uri: String): Long
    suspend fun delete(uri: String)
    suspend fun rename(uri: String, name: String): String
}
