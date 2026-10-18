package com.example.linkgrab.feature.home

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkgrab.AppContainer
import com.example.linkgrab.core.*
import com.example.linkgrab.domain.*
import com.example.linkgrab.service.DownloadService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AnalysisState(val loading: Boolean = false, val metadata: VideoMetadata? = null,
    val error: String? = null, val details: String? = null)
class HomeViewModel(private val graph: AppContainer, private val saved: SavedStateHandle) : ViewModel() {
    val link = saved.getStateFlow("link", "")
    private val mutableAnalysis = MutableStateFlow(AnalysisState())
    val analysis = mutableAnalysis.asStateFlow()
    val events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val sharedNavigation = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val mutableSubmitting = MutableStateFlow(false)
    val submitting = mutableSubmitting.asStateFlow()
    private var job: Job? = null
    fun setLink(value: String) { saved["link"] = value.take(8192) }
    fun receiveShare(value: String) { job?.cancel(); setLink(LinkParser.extract(value)); mutableAnalysis.value = AnalysisState(); sharedNavigation.tryEmit(Unit) }
    fun analyze() {
        val url = LinkParser.extract(link.value)
        if (!LinkParser.isValid(url)) { mutableAnalysis.value = AnalysisState(error = "Вставь корректную ссылку на видео."); return }
        setLink(url)
        job?.cancel()
        job = viewModelScope.launch {
            mutableAnalysis.value = AnalysisState(loading = true)
            try { mutableAnalysis.value = AnalysisState(metadata = graph.engine.analyze(url)) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { mutableAnalysis.value = AnalysisState(error = ErrorMapper.message(error), details = error.message?.takeLast(1600)) }
        }
    }
    fun closePreview() { job?.cancel(); mutableAnalysis.value = AnalysisState() }
    fun enqueue(context: Context, options: DownloadOptions, done: () -> Unit) {
        val metadata = analysis.value.metadata ?: return
        if (mutableSubmitting.value) return
        mutableSubmitting.value = true
        viewModelScope.launch {
            try {
                graph.ready.await()
                graph.downloads.enqueue(metadata, options)
                runCatching { DownloadService.start(context.applicationContext) }.onFailure {
                    events.emit("Очередь сохранена. Открой загрузки и нажми «Запустить очередь».")
                }
                mutableAnalysis.value = AnalysisState(); setLink("")
                events.emit("Добавлено в очередь"); done()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { events.emit(ErrorMapper.message(error)) }
            finally { mutableSubmitting.value = false }
        }
    }
}
