package com.example.linkgrab.core

import java.io.IOException

object ErrorMapper {
    fun message(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("space", true) || raw.contains("ENOSPC", true) -> "Не хватает свободного места на устройстве."
            raw.contains("Unsupported URL", true) -> "Сервис не поддерживает эту ссылку. Попробуй прямую ссылку на видео."
            raw.contains("DRM", true) -> "Видео защищено DRM и не может быть сохранено."
            raw.contains("private", true) || raw.contains("sign in", true) || raw.contains("login", true) -> "Видео закрыто или требует входа. Попробуй публичную ссылку."
            raw.contains("403") || raw.contains("429") -> "Сайт отклонил запрос к видео (403/429). Попробуй обновить yt-dlp или сменить сеть."
            raw.contains("Requested format", true) || raw.contains("codec", true) -> "Этот формат недоступен. Попробуй «Лучшее» или другое качество."
            error is IOException || raw.contains("resolve", true) || raw.contains("timed out", true) ||
                raw.contains("name resolution", true) || raw.contains("Name or service not known", true) ||
                raw.contains("connection", true) -> "Не удалось подключиться к серверу видео. Проверь сеть, DNS или VPN."
            else -> raw.lineSequence().lastOrNull { it.isNotBlank() }?.take(240)
                ?: "Не удалось выполнить операцию. Попробуй ещё раз."
        }
    }

    /** Keep a short redacted exception excerpt so real-device failures are diagnosable. */
    fun failure(error: Throwable): String {
        val title = message(error)
        val diagnostic = error.message.orEmpty()
            .replace(Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE), "[ссылка скрыта]")
            .replace(Regex("(?i)(authorization|cookie|password|token|secret|api[_-]?key)[=:]\\s*[^\\s,;]+"),
                "$1=[скрыто]")
            .replace(Regex("(?i)(sig|signature|sessionid|auth)[=:]\\s*[^\\s,;]+"), "$1=[скрыто]")
            .trim().takeLast(1200)
        val cause = if (diagnostic.isBlank()) error.javaClass.simpleName else diagnostic
        return "$title\n\nДиагностика: $cause"
    }
}
