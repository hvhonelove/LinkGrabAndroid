package com.example.linkgrab.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkgrab.AppContainer
import com.example.linkgrab.core.ErrorMapper
import com.example.linkgrab.data.settings.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SettingsViewModel(private val graph: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings?> = graph.settings.flow.map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val mutableVersion = MutableStateFlow("Проверяем…")
    val version = mutableVersion.asStateFlow()
    private val mutableUpdating = MutableStateFlow(false)
    val updating = mutableUpdating.asStateFlow()
    val events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private var checkingVersion = false
    init { action { mutableVersion.value = withContext(Dispatchers.IO) { graph.engine.version() } } }
    fun text(key: String, value: String) = action { graph.settings.text(key, value) }
    fun flag(key: String, value: Boolean) = action { graph.settings.flag(key, value) }
    fun quality(value: Int) = action { graph.settings.quality(value) }
    fun refreshVersion() {
        if (checkingVersion) return
        checkingVersion = true
        val previous = mutableVersion.value
        mutableVersion.value = "Проверяем…"
        viewModelScope.launch {
            try { mutableVersion.value = graph.engine.checkVersion() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableVersion.value = previous; events.emit(ErrorMapper.message(e)) }
            finally { checkingVersion = false }
        }
    }
    fun updateEngine() {
        if (mutableUpdating.value) return
        mutableUpdating.value = true
        viewModelScope.launch {
            try {
                mutableVersion.value = graph.engineUpdates.manualUpdate()
                events.emit("Загрузчик обновлён")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { events.emit(ErrorMapper.message(e)) }
            finally { mutableUpdating.value = false }
        }
    }
    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { events.emit(ErrorMapper.message(e)) }
        }
    }
}
