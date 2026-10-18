package com.example.linkgrab.domain

import java.io.File

interface VideoEngine {
    suspend fun analyze(url: String): VideoMetadata
    suspend fun download(id: Long, url: String, options: DownloadOptions, folder: File,
        progress: (EngineProgress) -> Unit): File
    suspend fun cancel(id: Long)
    suspend fun update(): String
    fun version(): String
    suspend fun checkVersion(): String
}
