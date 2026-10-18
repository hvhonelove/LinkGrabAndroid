package com.example.linkgrab.feature.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkgrab.AppContainer
import com.example.linkgrab.core.ErrorMapper
import com.example.linkgrab.service.DownloadService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DownloadsViewModel(private val graph: AppContainer) : ViewModel() {
    val items = graph.downloads.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    fun retry(context: Context, id: Long) = action {
        graph.ready.await(); graph.downloads.retry(id); DownloadService.start(context.applicationContext)
    }
    fun startQueue(context: Context) = action { graph.ready.await(); DownloadService.start(context.applicationContext) }
    fun pause(id: Long) = action { graph.downloads.pause(id) }
    fun cancel(id: Long) = action { graph.downloads.cancel(id) }
    fun next(id: Long) = action { graph.downloads.moveNext(id); events.emit("Будет загружено следующим") }
    fun rename(id: Long, title: String) = action { graph.downloads.rename(id, title) }
    fun remove(id: Long, file: Boolean) = action { graph.downloads.remove(id, file) }
    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { events.emit(ErrorMapper.message(e)) }
        }
    }
}
