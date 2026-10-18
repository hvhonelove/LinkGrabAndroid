package com.example.linkgrab.service

import com.example.linkgrab.AppContainer
import com.example.linkgrab.core.ErrorMapper
import com.example.linkgrab.data.db.DownloadEntity
import com.example.linkgrab.domain.TaskStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first

class QueueProcessor(private val graph: AppContainer, private val notifications: DownloadNotifications) {
    private val repo = graph.downloads
    suspend fun runOne(item: DownloadEntity) = coroutineScope {
        if (repo.dao.claim(item.id) != 1) return@coroutineScope
        val progressUpdates = Channel<com.example.linkgrab.domain.EngineProgress>(Channel.CONFLATED)
        val collector = launch {
            for (p in progressUpdates) {
                repo.dao.progress(item.id, p.percent, p.speed, p.eta, p.phase)
                notifications.progress(item.title, p.percent.toInt(), item.id)
                delay(450)
            }
        }
        val stopper = launch(Dispatchers.IO) {
            while (isActive) {
                delay(250)
                if (repo.dao.get(item.id)?.status != TaskStatus.RUNNING) graph.engine.cancel(item.id)
            }
        }
        try {
            graph.engineUpdates.ensureReady { phase ->
                progressUpdates.trySend(com.example.linkgrab.domain.EngineProgress(-1f, 0, "", phase))
            }
            if (repo.dao.get(item.id)?.status != TaskStatus.RUNNING) return@coroutineScope
            val file = try {
                graph.engine.download(item.id, item.url, item.options(), repo.workFolder(item.id)) { progressUpdates.trySend(it) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (repo.dao.get(item.id)?.status != TaskStatus.RUNNING) throw error
                val retry = graph.engineUpdates.tryRepair(error) { phase ->
                    progressUpdates.trySend(com.example.linkgrab.domain.EngineProgress(-1f, 0, "", phase))
                }
                if (!retry || repo.dao.get(item.id)?.status != TaskStatus.RUNNING) throw error
                graph.engine.download(item.id, item.url, item.options(), repo.workFolder(item.id)) { progressUpdates.trySend(it) }
            }
            collector.cancelAndJoin()
            if (repo.dao.get(item.id)?.status != TaskStatus.RUNNING) return@coroutineScope
            repo.dao.progress(item.id, 100f, "", 0, "Обработка и сохранение")
            val media = graph.storage.publish(item, file,
                created = { uri, name, mime -> repo.dao.publishing(item.id, uri, name, mime) },
                stillRunning = { repo.dao.get(item.id)?.status == TaskStatus.RUNNING })
            repo.dao.publishing(item.id, media.uri, media.name, media.mime)
            repo.dao.complete(item.id, media.bytes, System.currentTimeMillis())
            if (graph.settings.flow.first().notifications) notifications.finished(item.title, media.uri, media.mime, item.id)
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                val current = repo.dao.get(item.id)
                if (current?.status == TaskStatus.RUNNING) repo.dao.status(item.id, TaskStatus.PAUSED, "Прервано — можно продолжить")
            }
            if (!currentCoroutineContext().isActive) throw error
        } catch (error: Exception) {
            val current = repo.dao.get(item.id)
            if (current?.status == TaskStatus.RUNNING) repo.dao.update(current.copy(status = TaskStatus.FAILED,
                error = ErrorMapper.failure(error), phase = "Ошибка", speed = "", eta = 0))
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                stopper.cancelAndJoin(); collector.cancelAndJoin(); progressUpdates.close()
                graph.engine.cancel(item.id)
                val state = repo.dao.get(item.id)?.status
                if (state == null || state in setOf(TaskStatus.CANCELED, TaskStatus.COMPLETED)) repo.workFolder(item.id).deleteRecursively()
            }
        }
    }
}
