package com.example.linkgrab.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.linkgrab.domain.LEGAL_NOTICE
import com.example.linkgrab.ui.components.PrimaryButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun OnboardingScreen(finish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Твоя лента.\nТвоя коллекция.", "Скачивай.\nЗанимайся своим.", "Бесплатно.\nИ с уважением.")
    val descriptions = listOf("Вставь ссылку, выбери видео или аудио — и сохрани любимое на телефон.",
        "Очередь работает в фоне. Видео сохраняются в галерею, музыка — в папку Music / LinkGrab.",
        "Без аккаунта, рекламы и платных функций. Обработка происходит на устройстве.")
    val icons = listOf(Icons.Outlined.PlayCircle, Icons.Outlined.Downloading, Icons.Outlined.FavoriteBorder)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("LinkGrab", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Surface(Modifier.fillMaxWidth().height(240.dp), shape = RoundedCornerShape(40.dp), color = Color(0xFF8253F5)) {
            Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFFA97BFF), Color(0xFF6937DC)))), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(32.dp), color = Color.White.copy(alpha = .18f)) {
                    Icon(icons[page], null, Modifier.padding(28.dp).size(82.dp), tint = Color.White)
                }
            }
        }
        AnimatedContent(page, label = "onboarding") { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text(titles[index], style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text(descriptions[index], style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (page == 2) Text(LEGAL_NOTICE, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) { Surface(Modifier.size(if (it == page) 22.dp else 7.dp, 7.dp), shape = RoundedCornerShape(50),
                color = if (it == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) {} }
        }
        PrimaryButton(if (page < 2) "Дальше" else "Начать", Icons.Outlined.ArrowForward,
            Modifier.testTag("onboarding-next")) { if (page < 2) page++ else finish() }
    }
}
