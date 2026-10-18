package com.example.linkgrab.feature.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.linkgrab.core.*
import com.example.linkgrab.data.db.DownloadEntity
import com.example.linkgrab.domain.*
import com.example.linkgrab.ui.components.*
import java.text.DateFormat
import java.util.Date

enum class FileAction { OPEN, SHARE, FOLDER, COPY_LINK }
@Composable
fun FileDetailsScreen(item: DownloadEntity, back: () -> Unit, action: (FileAction) -> Unit,
    rename: (String) -> Unit, remove: (Boolean) -> Unit, retry: () -> Unit, pause: () -> Unit, cancel: () -> Unit) {
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var name by remember(item.title) { mutableStateOf(item.title) }
    Column(Modifier.fillMaxSize().testTag("file-details")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, "Назад") }
            Text("Детали файла", style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            MediaThumbnail(item.thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9), true)
            SourceBadge(item.source)
            Text(item.title, style = MaterialTheme.typography.headlineMedium)
            Text(statusLabel(item), color = MaterialTheme.colorScheme.primary)
            PremiumCard {
                DetailRow("Размер", bytesLabel(item.bytes))
                DetailRow("Тип", if (item.kind == MediaKind.AUDIO) "Аудио" else "Видео · ${qualityLabel(item.quality)}")
                DetailRow("Дата", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.createdAt)))
                if (item.fileName.isNotBlank()) DetailRow("Имя файла", item.fileName)
                if (item.uploader.isNotBlank()) DetailRow("Автор", item.uploader)
                DetailRow("Папка", if (item.kind == MediaKind.AUDIO) "Music / LinkGrab" else "Movies / LinkGrab")
                item.error?.let { report ->
                    Text(report.substringBefore("\n"), color = MaterialTheme.colorScheme.error)
                    if (report.contains("Диагностика:")) {
                        var showTechnical by remember { mutableStateOf(false) }
                        TextButton(onClick = { showTechnical = !showTechnical }) {
                            Text(if (showTechnical) "Скрыть техническую причину" else "Показать техническую причину")
                        }
                        if (showTechnical) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(report.substringAfter("Диагностика:").trim(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            if (item.status == TaskStatus.COMPLETED) {
                PrimaryButton("Открыть файл", Icons.Outlined.PlayArrow) { action(FileAction.OPEN) }
                OutlinedButton(onClick = { action(FileAction.SHARE) }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(8.dp)); Text("Поделиться")
                }
                TextButton(onClick = { renameDialog = true }, modifier = Modifier.fillMaxWidth().testTag("rename")) { Text("Переименовать") }
                TextButton(onClick = { action(FileAction.FOLDER) }, modifier = Modifier.fillMaxWidth()) { Text("Показать расположение") }
            }
            if (item.status == TaskStatus.RUNNING) PrimaryButton("Пауза", Icons.Outlined.Pause, click = pause)
            else if (item.url.isNotBlank() && item.status != TaskStatus.QUEUED) PrimaryButton(
                if (item.status == TaskStatus.COMPLETED) "Скачать ещё раз" else "Продолжить / повторить", Icons.Outlined.Refresh, click = retry)
            if (item.status in setOf(TaskStatus.RUNNING, TaskStatus.PAUSED, TaskStatus.QUEUED)) TextButton(onClick = cancel) { Text("Отменить загрузку") }
            if (item.url.isNotBlank()) TextButton(onClick = { action(FileAction.COPY_LINK) }) { Text("Скопировать исходную ссылку") }
            TextButton(onClick = { deleteDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Удалить") }
        }
    }
    if (renameDialog) AlertDialog(onDismissRequest = { renameDialog = false }, title = { Text("Имя файла") }, text = {
        OutlinedTextField(name, { name = it }, label = { Text("Название без расширения") }, modifier = Modifier.testTag("rename-input"))
    }, confirmButton = { TextButton(onClick = { rename(name); renameDialog = false }, enabled = name.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Отмена") } })
    if (deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text("Удалить загрузку?") },
        text = { Text(if (item.status == TaskStatus.COMPLETED) "Можно удалить файл вместе с записью или убрать только запись из истории."
            else "Загрузка остановится, временные файлы и запись будут удалены.") },
        confirmButton = { TextButton(onClick = { remove(true); deleteDialog = false }) { Text(if (item.status == TaskStatus.COMPLETED) "Удалить файл" else "Удалить") } },
        dismissButton = { Row {
            if (item.status == TaskStatus.COMPLETED) TextButton(onClick = { remove(false); deleteDialog = false }) { Text("Только запись") }
            TextButton(onClick = { deleteDialog = false }) { Text("Отмена") }
        } })
}
@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
