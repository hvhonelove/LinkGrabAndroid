package com.example.linkgrab.core

import java.util.Locale
fun qualityLabel(height: Int) = if (height == 0) "Лучшее" else "${height}p"
fun bytesLabel(bytes: Long): String = when {
    bytes <= 0 -> "Размер неизвестен"
    bytes < 1_048_576 -> "${bytes / 1024} КБ"
    bytes < 1_073_741_824 -> String.format(Locale.getDefault(), "%.1f МБ", bytes / 1048576.0)
    else -> String.format(Locale.getDefault(), "%.2f ГБ", bytes / 1073741824.0)
}
fun durationLabel(seconds: Long): String = if (seconds <= 0) "Длительность неизвестна" else
    if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
fun safeFileName(value: String): String = value.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
    .trim().trim('.').take(100).ifBlank { "LinkGrab" }
