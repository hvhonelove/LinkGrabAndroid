package com.example.linkgrab.feature.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.linkgrab.core.*
import com.example.linkgrab.data.settings.AppSettings
import com.example.linkgrab.domain.*
import com.example.linkgrab.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(metadata: VideoMetadata, settings: AppSettings, submitting: Boolean,
    back: () -> Unit, download: (DownloadOptions) -> Unit) {
    var quality by rememberSaveable(metadata.url) { mutableIntStateOf(settings.quality.takeIf { it in metadata.qualities } ?: 0) }
    var kindName by rememberSaveable(metadata.url) { mutableStateOf(settings.kind.name) }
    val kind = MediaKind.valueOf(kindName)
    var formatName by rememberSaveable(metadata.url, kindName) {
        mutableStateOf(if (kind == MediaKind.AUDIO) settings.audioFormat.name else ContainerFormat.AUTO.name)
    }
    val format = ContainerFormat.valueOf(formatName)
    var sheet by rememberSaveable { mutableStateOf(false) }
    val options = DownloadOptions(quality, kind, format)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, "Назад") }
            Text("Предпросмотр", style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            MediaThumbnail(metadata.thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9), large = true)
            SourceBadge(metadata.source)
            Text(metadata.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("preview-title"))
            Text(listOf(metadata.uploader, durationLabel(metadata.duration)).filter { it.isNotBlank() }.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            PremiumCard {
                Text("Что сохранить", style = MaterialTheme.typography.titleMedium)
                ChoiceRow(MediaKind.entries, kind, { if (it == MediaKind.VIDEO) "Видео" else "Только аудио" }) { kindName = it.name }
                if (kind == MediaKind.VIDEO) {
                    Text("Качество", style = MaterialTheme.typography.titleMedium)
                    ChoiceRow(metadata.qualities, quality, ::qualityLabel) { quality = it }
                    Text("Показаны доступные разрешения. «Лучшее» сохраняет максимум источника.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else Text("M4A или MP3. При необходимости звук будет преобразован на устройстве.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (metadata.estimate(options) > 0) Text("Примерный размер: ${bytesLabel(metadata.estimate(options))}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shadowElevation = 8.dp) {
            Column(Modifier.padding(20.dp)) {
                PrimaryButton(if (kind == MediaKind.VIDEO) "Скачать видео" else "Скачать аудио", Icons.Outlined.Download,
                    Modifier.testTag("download-options"), enabled = !submitting) { sheet = true }
            }
        }
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { if (!submitting) sheet = false }) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Параметры загрузки", style = MaterialTheme.typography.headlineMedium)
            Text(if (kind == MediaKind.VIDEO) "Контейнер видео" else "Формат аудио", style = MaterialTheme.typography.titleMedium)
            val formats = if (kind == MediaKind.VIDEO) listOf(ContainerFormat.AUTO, ContainerFormat.MP4, ContainerFormat.WEBM)
                else listOf(ContainerFormat.M4A, ContainerFormat.MP3)
            ChoiceRow(formats, format, { if (it == ContainerFormat.AUTO) "Авто" else it.name }) { formatName = it.name }
            Text(if (kind == MediaKind.VIDEO) "Авто предпочитает MP4 и при необходимости использует WebM или MKV. Принудительный контейнер может быть несовместим с кодеком."
                else "Перекодирование может увеличить время загрузки. Размер после преобразования может отличаться от оценки.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (kind == MediaKind.VIDEO) "Movies / LinkGrab" else "Music / LinkGrab", style = MaterialTheme.typography.bodyMedium)
            PrimaryButton(if (submitting) "Добавляем…" else "Добавить в очередь", Icons.Outlined.AddToQueue,
                Modifier.testTag("enqueue"), enabled = !submitting) { download(options) }
        }
    }
}
