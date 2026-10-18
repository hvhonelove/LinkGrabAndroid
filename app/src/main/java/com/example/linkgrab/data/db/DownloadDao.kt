package com.example.linkgrab.data.db

import androidx.room.*
import com.example.linkgrab.domain.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC") fun observe(): Flow<List<DownloadEntity>>
    @Query("SELECT * FROM downloads WHERE id = :id") suspend fun get(id: Long): DownloadEntity?
    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY priority DESC, createdAt ASC LIMIT 1")
    suspend fun next(): DownloadEntity?
    @Query("SELECT * FROM downloads WHERE status = 'RUNNING'") suspend fun running(): List<DownloadEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(item: DownloadEntity): Long
    @Update suspend fun update(item: DownloadEntity)
    @Query("DELETE FROM downloads WHERE id = :id") suspend fun delete(id: Long)
    @Query("UPDATE downloads SET status = :status, phase = :phase, speed = '', eta = 0 WHERE id = :id AND status IN ('QUEUED','RUNNING','PAUSED','FAILED','CANCELED')")
    suspend fun status(id: Long, status: TaskStatus, phase: String)
    @Query("UPDATE downloads SET progress = :value, speed = :speed, eta = :eta, phase = :phase WHERE id = :id AND status = 'RUNNING'")
    suspend fun progress(id: Long, value: Float, speed: String, eta: Long, phase: String)
    @Query("UPDATE downloads SET uri = :uri, fileName = :name, mime = :mime WHERE id = :id AND status = 'RUNNING'")
    suspend fun publishing(id: Long, uri: String, name: String, mime: String)
    @Query("UPDATE downloads SET status = 'COMPLETED', progress = 100, bytes = :bytes, completedAt = :now, phase = 'Сохранено', speed = '', eta = 0, error = NULL WHERE id = :id")
    suspend fun complete(id: Long, bytes: Long, now: Long)
    @Query("UPDATE downloads SET status = 'RUNNING', phase = 'Подготовка', error = NULL WHERE id = :id AND status = 'QUEUED'")
    suspend fun claim(id: Long): Int
    @Query("UPDATE downloads SET priority = (SELECT COALESCE(MAX(priority), 0) + 1 FROM downloads) WHERE id = :id AND status = 'QUEUED'")
    suspend fun moveNext(id: Long)
}
