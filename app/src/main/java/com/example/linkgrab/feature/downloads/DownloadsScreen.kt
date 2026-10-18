package com.example.linkgrab.feature.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.linkgrab.core.*
import com.example.linkgrab.data.db.DownloadEntity
import com.example.linkgrab.domain.*
import com.example.linkgrab.ui.components.*
import java.text.DateFormat
import java.util.Date

@Composable
fun DownloadsScreen(items: List<DownloadEntity>, details: (Long) -> Unit, retry: (Long) -> Unit,
    pause: (Long) -> Unit, cancel: (Long) -> Unit, next: (Long) -> Unit, startQueue: () -> Unit, home: () -> Unit,
    fileAction: (DownloadEntity, FileAction) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val active = setOf(TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.PAUSED)
    val filtered = items.filter {
        (when (filter) { 0 -> it.status in active; 1 -> it.status == TaskStatus.COMPLETED; else -> it.status in setOf(TaskStatus.FAILED, TaskStatus.CANCELED) }) &&
            (query.isBlank() || it.title.contains(query, true) || it.source.contains(query, true))
    }
    LazyColumn(Modifier.fillMaxSize().testTag("downloads"), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("Загрузки", "Твоя коллекция. Всегда под рукой.") }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("history-search"),
            placeholder = { Text("Поиск по названию или сайту") }, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) }
        item { ChoiceRow(listOf(0, 1, 2), filter, { listOf("Активные", "Готовые", "Неудачные")[it] }) { filter = it } }
        if (filter == 0 && items.any { it.status == TaskStatus.QUEUED } && items.none { it.status == TaskStatus.RUNNING }) item {
            PrimaryButton("Запустить очередь", Icons.Outlined.PlayArrow, click = startQueue)
        }
        if (filtered.isEmpty()) item {
            EmptyState(if (query.isNotBlank()) "Ничего не найдено" else when (filter) {
                0 -> "Всё спокойно"; 1 -> "Коллекция начинается здесь"; else -> "Ошибок пока нет"
            }, if (query.isNotBlank()) "Попробуй другое название или источник." else when (filter) {
                0 -> "Добавь видео по ссылке. Очередь продолжит работу, даже если свернуть приложение."
                1 -> "Готовые видео и аудио появятся здесь после скачивания."
                else -> "Здесь можно повторить неудачные или отменённые загрузки."
            }, button = if (filter < 2 && query.isBlank()) "Добавить ссылку" else null, click = home)
        }
        items(filtered, key = { it.id }) { item ->
            val menu = buildList<Pair<String, () -> Unit>> {
                add("Детали файла" to { details(item.id) })
                if (item.status == TaskStatus.COMPLETED) {
                    add("Открыть" to { fileAction(item, FileAction.OPEN) })
                    add("Поделиться" to { fileAction(item, FileAction.SHARE) })
                }
                if (item.status in setOf(TaskStatus.FAILED, TaskStatus.PAUSED, TaskStatus.CANCELED)) add("Продолжить / повторить" to { retry(item.id) })
                if (item.status == TaskStatus.RUNNING) add("Пауза" to { pause(item.id) })
                if (item.status == TaskStatus.QUEUED) add("Следующим" to { next(item.id) })
                if (item.status in active) add("Отменить" to { cancel(item.id) })
            }
            DownloadCard(item, { details(item.id) }, menuActions = menu, actions = {
                when (item.status) {
                    TaskStatus.RUNNING -> TextButton(onClick = { pause(item.id) }) { Icon(Icons.Outlined.Pause, null); Text("Пауза") }
                    TaskStatus.PAUSED -> TextButton(onClick = { retry(item.id) }) { Icon(Icons.Outlined.PlayArrow, null); Text("Продолжить") }
                    TaskStatus.FAILED, TaskStatus.CANCELED -> TextButton(onClick = { retry(item.id) }) { Icon(Icons.Outlined.Refresh, null); Text("Повторить") }
                    TaskStatus.QUEUED -> TextButton(onClick = { next(item.id) }) { Text("Следующим") }
                    else -> TextButton(onClick = { details(item.id) }) { Text("Открыть детали") }
                }
                if (item.status in active) IconButton(onClick = { cancel(item.id) }) { Icon(Icons.Outlined.Close, "Отменить загрузку") }
            })
        }
    }
}
@Composable
fun DownloadCard(item: DownloadEntity, openDetails: () -> Unit, menuActions: List<Pair<String, () -> Unit>> = emptyList(),
    actions: (@Composable RowScope.() -> Unit)? = null) {
    PremiumCard(Modifier.clickable(onClick = openDetails).testTag("task-${item.id}")) {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaThumbnail(item.thumbnail, Modifier.size(76.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${item.source} · ${if (item.kind == MediaKind.AUDIO) "Аудио" else qualityLabel(item.quality)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(statusLabel(item), style = MaterialTheme.typography.bodySmall,
                    color = if (item.status == TaskStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            if (menuActions.isEmpty()) Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.MoreVert, "Действия с загрузкой") }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        menuActions.forEach { (label, command) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; command() })
                        }
                    }
                }
            }
        }
        if (item.status == TaskStatus.RUNNING) {
            if (item.progress < 0) LinearProgressIndicator(Modifier.fillMaxWidth())
            else LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (item.progress < 0) item.phase else "${item.progress.toInt()}% · ${item.speed.ifBlank { "Обработка" }}", style = MaterialTheme.typography.bodySmall)
                if (item.eta > 0) Text("≈ ${item.eta} с", style = MaterialTheme.typography.bodySmall)
            }
        } else Text(if (item.status == TaskStatus.COMPLETED) "${bytesLabel(item.bytes)} · ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(item.createdAt))}"
            else item.error ?: item.phase, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (item.status == TaskStatus.RUNNING && item.bytes > 0) Text("≈ ${bytesLabel(item.bytes)}", style = MaterialTheme.typography.bodySmall)
        if (actions != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}
fun statusLabel(item: DownloadEntity): String = when (item.status) {
    TaskStatus.QUEUED -> "В очереди"; TaskStatus.RUNNING -> item.phase; TaskStatus.PAUSED -> "На паузе"
    TaskStatus.COMPLETED -> "Сохранено"; TaskStatus.FAILED -> "Не удалось скачать"; TaskStatus.CANCELED -> "Отменено"
}
