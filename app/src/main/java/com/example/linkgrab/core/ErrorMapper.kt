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
            raw.contains("403") || raw.contains("429") -> "Сайт ограничил доступ. Попробуй позже или обнови загрузчик."
            error is IOException || raw.contains("resolve", true) || raw.contains("timed out", true) || raw.contains("name resolution", true) || raw.contains("Name or service not known", true) || raw.contains("connection", true) -> "Проверь подключение и повтори попытку."
            raw.contains("Requested format", true) || raw.contains("codec", true) -> "Этот формат недоступен. Попробуй «Авто» или другое качество."
            else -> raw.lineSequence().lastOrNull { it.isNotBlank() }?.take(240) ?: "Не удалось выполнить операцию. Попробуй ещё раз."
        }
    }
}
