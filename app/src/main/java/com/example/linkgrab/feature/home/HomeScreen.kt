package com.example.linkgrab.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkgrab.data.db.DownloadEntity
import com.example.linkgrab.domain.TaskStatus
import com.example.linkgrab.ui.components.*
import com.example.linkgrab.feature.downloads.DownloadCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(link: String, state: AnalysisState, recent: List<DownloadEntity>, clipboardSuggestion: String?,
    onLink: (String) -> Unit, paste: () -> Unit, analyze: () -> Unit, showDownloads: () -> Unit,
    details: (Long) -> Unit, acceptClipboard: () -> Unit, dismissClipboard: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("home"), contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Outlined.Downloading, null, Modifier.padding(11.dp).size(26.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column { Text("LinkGrab", style = MaterialTheme.typography.titleLarge)
                    Text("Видео. Музыка. Твои.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = showDownloads) { Icon(Icons.Outlined.VideoLibrary, "Открыть загрузки") }
            }
        }
        item { HeroCard() }
        if (clipboardSuggestion != null) item {
            PremiumCard {
                Text("В буфере есть ссылка", style = MaterialTheme.typography.titleMedium)
                Row { TextButton(onClick = acceptClipboard) { Text("Вставить") }
                    TextButton(onClick = dismissClipboard) { Text("Не сейчас") } }
            }
        }
        item {
            PremiumCard(Modifier.animateContentSize()) {
                Text("Ссылка на видео", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = link, onValueChange = onLink, modifier = Modifier.fillMaxWidth().testTag("link-input"),
                    placeholder = { Text("https://…") }, label = { Text("Вставь ссылку") }, maxLines = 3,
                    shape = RoundedCornerShape(18.dp), isError = state.error != null,
                    trailingIcon = { if (link.isNotEmpty()) IconButton(onClick = { onLink("") }) { Icon(Icons.Outlined.Close, "Очистить ссылку") } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
                    keyboardActions = KeyboardActions(onGo = { analyze() }))
                OutlinedButton(onClick = paste, Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Outlined.ContentPaste, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("Вставить из буфера")
                }
                PrimaryButton(if (state.loading) "Анализируем ссылку…" else "Анализировать ссылку", Icons.Outlined.AutoAwesome,
                    Modifier.testTag("analyze"), enabled = !state.loading, click = analyze)
                if (state.loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Получаем название, превью и доступные форматы", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    if (state.details != null) {
                        var expanded by remember { mutableStateOf(false) }
                        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Скрыть детали" else "Технические детали") }
                        if (expanded) Text(state.details, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("YouTube", "TikTok", "VK Видео", "X / Twitter", "+ сайты").forEach {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                        Text(it, Modifier.padding(horizontal = 14.dp, vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        item { SectionTitle("Недавние загрузки", "Все", showDownloads) }
        val completed = recent.filter { it.status == TaskStatus.COMPLETED }.take(3)
        if (completed.isEmpty()) item {
            EmptyState("Твои видео будут здесь", "Вставь ссылку выше или отправь её в LinkGrab через меню «Поделиться».")
        } else items(completed, key = { it.id }) { item ->
            DownloadCard(item, openDetails = { details(item.id) })
        }
        item {
            Text("Сохраняй только свой контент или видео, на скачивание которых есть разрешение.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
private fun HeroCard() {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().clipForHero().background(Brush.linearGradient(listOf(Color(0xFF8D5AF8), Color(0xFF6333CE))))
        .padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Сохрани то,\nчто нравится.", fontSize = 29.sp, lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-.6).sp)
                Text("Одна ссылка. Без подписки.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE5D9FF))
            }
            Box(Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.size(62.dp, 82.dp).rotate(16f), shape = RoundedCornerShape(20.dp), color = Color(0xFFB998FF)) {}
                Surface(Modifier.size(62.dp, 82.dp).rotate(-9f), shape = RoundedCornerShape(20.dp), color = Color.White,
                    shadowElevation = 12.dp) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PlayCircle, null,
                        Modifier.size(38.dp), tint = Color(0xFF8253F5)) }
                }
            }
        }
    }
}
private fun Modifier.clipForHero() = this.clip(RoundedCornerShape(28.dp))
