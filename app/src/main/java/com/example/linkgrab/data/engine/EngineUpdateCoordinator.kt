package com.example.linkgrab.data.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.linkgrab.data.settings.SettingsStore
import com.example.linkgrab.domain.VideoEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single gate for automatic and manual updates. Does not prevent using the bundled extractor
 * if the network/update endpoint is unavailable. The opt-out preference is always respected.
 */
class EngineUpdateCoordinator(
    private val context: Context,
    private val engine: VideoEngine,
    private val settings: SettingsStore
) {
    private val mutex = Mutex()

    private fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Runs at first link analysis / download; success is cached for 24h, failures for 1h. */
    suspend fun ensureReady(status: (String) -> Unit = {}) {
        if (!isOnline()) return
        val initial = settings.flow.first()
        if (!initial.autoUpdate || !EngineUpdatePolicy.isDue(
                System.currentTimeMillis(), initial.lastEngineCheck, initial.lastEngineUpdate
            )) return

        mutex.withLock {
            val latest = settings.flow.first()
            if (!latest.autoUpdate || !EngineUpdatePolicy.isDue(
                    System.currentTimeMillis(), latest.lastEngineCheck, latest.lastEngineUpdate
                )) return@withLock
            status("Подготавливаем загрузчик для поддерживаемых сайтов…")
            // Persist attempt *before* the call to avoid network storms after a crash.
            settings.engineChecked()
            try {
                engine.update()
                settings.engineUpdated()
                status("Загрузчик готов. Получаем видео…")
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                status("Не удалось проверить обновления. Пробуем встроенный загрузчик…")
            }
        }
    }

    /** Exactly one refresh-and-retry on likely extractor failures, never on storage/FFmpeg errors. */
    suspend fun tryRepair(error: Exception, status: (String) -> Unit = {}): Boolean {
        if (!EngineUpdatePolicy.isRecoverableExtractorFailure(error.message.orEmpty()) || !isOnline()) return false
        return mutex.withLock {
            val state = settings.flow.first()
            if (!state.autoUpdate || !EngineUpdatePolicy.isDue(
                    System.currentTimeMillis(), state.lastEngineCheck, state.lastEngineUpdate
                )) return@withLock false
            status("Сайт изменился. Проверяем обновление загрузчика…")
            settings.engineChecked()
            try {
                engine.update()
                settings.engineUpdated()
                status("Повторяем попытку с актуальным загрузчиком…")
                true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                status("Не удалось обновить движок; покажем причину ошибки.")
                false
            }
        }
    }

    suspend fun manualUpdate(): String = mutex.withLock {
        val version = engine.update()
        settings.engineUpdated()
        version
    }
}
