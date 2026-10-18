package com.example.linkgrab.domain

interface DownloadManager {
    suspend fun enqueue(metadata: VideoMetadata, options: DownloadOptions): Long
    suspend fun retry(id: Long): Long
    suspend fun pause(id: Long)
    suspend fun cancel(id: Long)
    suspend fun moveNext(id: Long)
}
