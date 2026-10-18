package com.example.linkgrab.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
fun PrimaryButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true, click: () -> Unit) {
    Button(onClick = click, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Icon(icon, null, Modifier.size(21.dp)); Spacer(Modifier.width(9.dp)); Text(text)
    }
}
@Composable
fun PageHeading(title: String, subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}
@Composable
fun MediaThumbnail(url: String?, modifier: Modifier = Modifier, large: Boolean = false) {
    val shape = RoundedCornerShape(if (large) 24.dp else 16.dp)
    val fallback: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surfaceVariant))), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PlayCircle, "Превью видео", tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (large) 58.dp else 30.dp))
        }
    }
    Box(modifier.clip(shape)) {
        if (url == null) fallback() else SubcomposeAsyncImage(model = url, contentDescription = "Превью видео",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            loading = { fallback() }, error = { fallback() })
    }
}
@Composable
fun SourceBadge(source: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Text(source.ifBlank { "Видео" }, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
    }
}
@Composable
fun EmptyState(title: String, description: String, icon: ImageVector = Icons.Outlined.VideoLibrary,
    button: String? = null, click: () -> Unit = {}) {
    PremiumCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(20.dp).size(36.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (button != null) PrimaryButton(button, Icons.Outlined.Add, click = click)
        }
    }
}
@Composable
fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(selected == value, onClick = { onSelect(value) }, label = { Text(label(value)) },
                modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp))
        }
    }
}
@Composable
fun SectionTitle(title: String, action: String? = null, click: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (action != null) TextButton(onClick = click) { Text(action) }
    }
}
