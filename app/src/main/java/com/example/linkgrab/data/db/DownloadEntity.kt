package com.example.linkgrab.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.linkgrab.domain.*

@Entity(tableName = "downloads", indices = [Index("status"), Index(value = ["uri"], unique = true)])
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val thumbnail: String? = null,
    val uploader: String = "",
    val source: String = "",
    val duration: Long = 0,
    val quality: Int = 0,
    val kind: MediaKind = MediaKind.VIDEO,
    val format: ContainerFormat = ContainerFormat.AUTO,
    val status: TaskStatus = TaskStatus.QUEUED,
    val progress: Float = 0f,
    val speed: String = "",
    val eta: Long = 0,
    val phase: String = "В очереди",
    val bytes: Long = 0,
    val uri: String? = null,
    val fileName: String = "",
    val mime: String = "video/mp4",
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0,
    val priority: Int = 0
) {
    fun options() = DownloadOptions(quality, kind, format)
}
class EnumConverters {
    @TypeConverter fun status(value: String) = TaskStatus.valueOf(value)
    @TypeConverter fun status(value: TaskStatus) = value.name
    @TypeConverter fun kind(value: String) = MediaKind.valueOf(value)
    @TypeConverter fun kind(value: MediaKind) = value.name
    @TypeConverter fun format(value: String) = ContainerFormat.valueOf(value)
    @TypeConverter fun format(value: ContainerFormat) = value.name
}
