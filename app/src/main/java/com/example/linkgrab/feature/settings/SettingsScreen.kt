package com.example.linkgrab.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.linkgrab.BuildConfig
import com.example.linkgrab.core.qualityLabel
import com.example.linkgrab.data.settings.AppSettings
import com.example.linkgrab.domain.*
import com.example.linkgrab.ui.components.*

@Composable
fun SettingsScreen(settings: AppSettings, version: String, updating: Boolean,
    text: (String, String) -> Unit, flag: (String, Boolean) -> Unit, quality: (Int) -> Unit,
    update: () -> Unit, refreshVersion: () -> Unit, notificationPermission: () -> Unit, about: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("settings"), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageHeading("Настройки", "Сделай LinkGrab своим.") }
        item { PremiumCard {
            Text("Оформление", style = MaterialTheme.typography.titleMedium)
            ChoiceRow(ThemeMode.entries, settings.theme, { when (it) { ThemeMode.SYSTEM -> "Система"; ThemeMode.LIGHT -> "Светлая"; ThemeMode.DARK -> "Тёмная" } }) { text("theme", it.name) }
        } }
        item { PremiumCard {
            Text("По умолчанию", style = MaterialTheme.typography.titleMedium)
            ChoiceRow(listOf(0, 1080, 720, 480), settings.quality, ::qualityLabel, quality)
            ChoiceRow(MediaKind.entries, settings.kind, { if (it == MediaKind.VIDEO) "Видео" else "Аудио" }) { text("kind", it.name) }
            Text("Формат аудио", style = MaterialTheme.typography.bodyMedium)
            ChoiceRow(listOf(ContainerFormat.M4A, ContainerFormat.MP3), settings.audioFormat, { it.name }) { text("audioFormat", it.name) }
        } }
        item { PremiumCard {
            Text("Удобство", style = MaterialTheme.typography.titleMedium)
            SettingSwitch("Предлагать ссылку из буфера", "Только при открытии приложения. Вставка — после твоего нажатия.", settings.clipboard) { flag("clipboard", it) }
            SettingSwitch("Уведомлять о завершении", "Системное уведомление активной загрузки остаётся обязательным.", settings.notifications) { flag("notifications", it) }
            TextButton(onClick = notificationPermission) { Text("Разрешения уведомлений Android") }
        } }
        item { PremiumCard {
            Text("Загрузчик yt-dlp", style = MaterialTheme.typography.titleMedium)
            Text(version, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingSwitch("Обновлять автоматически", "Перед загрузкой, не чаще раза в сутки. Нужен доступ к GitHub.", settings.autoUpdate) { flag("autoUpdate", it) }
            PrimaryButton(if (updating) "Обновление…" else "Обновить загрузчик", Icons.Outlined.SystemUpdate,
                enabled = !updating, click = update)
            TextButton(onClick = refreshVersion) { Text("Проверить установленную версию") }
        } }
        item { PremiumCard {
            Text("Файлы остаются на телефоне", style = MaterialTheme.typography.titleMedium)
            Text("Видео: Movies / LinkGrab\nАудио: Music / LinkGrab\nВременные файлы: приватная папка приложения.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Разрешение на доступ ко всей галерее не требуется.", style = MaterialTheme.typography.bodySmall)
        } }
        item { OutlinedButton(onClick = about, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("О приложении и лицензиях") } }
        item { Text("LinkGrab ${BuildConfig.VERSION_NAME} · Бесплатно, без рекламы", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable
private fun SettingSwitch(title: String, hint: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, change)
    }
}
@Composable
fun AboutScreen(back: () -> Unit, licenses: () -> Unit, source: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, "Назад") }
            Text("О LinkGrab", style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { PageHeading("Сохраняй любимое.", "LinkGrab ${BuildConfig.VERSION_NAME}") }
            item { PremiumCard {
                Text("Бесплатно и открыто", style = MaterialTheme.typography.titleLarge)
                Text("Без аккаунтов, рекламы, подписок и собственного сервера. История хранится локально. Ссылки отправляются только источнику и службам, нужным самому источнику; обновления — с GitHub.")
            } }
            item { PremiumCard {
                Text("Использование контента", style = MaterialTheme.typography.titleLarge)
                Text("Скачивай только свой контент или материалы, на сохранение которых есть разрешение. Некоторые сайты ограничивают скачивание. Закрытый контент, DRM и видео с обязательной авторизацией могут не работать.")
                Text(LEGAL_NOTICE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            item { PremiumCard {
                Text("Что нужно знать", style = MaterialTheme.typography.titleLarge)
                Text("Поддержка сайта зависит от yt-dlp. Пауза сохраняет временные файлы; докачка зависит от источника. После остановки приложения системой открой очередь и нажми «Продолжить». Android и настройки батареи Samsung могут ограничивать фоновую работу.")
            } }
            item { PrimaryButton("Исходный код", Icons.Outlined.Code, click = source) }
            item { OutlinedButton(onClick = licenses, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Открытые лицензии") } }
        }
    }
}
