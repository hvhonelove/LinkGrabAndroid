package com.example.linkgrab

import android.net.Uri

object LinkParser {
    fun extract(text: String): String =
        Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE).find(text)?.value
            ?.trimEnd('.', ',', ')', ']', '!', '?', ';') ?: text.trim()

    fun isValid(url: String): Boolean = runCatching {
        val uri = Uri.parse(url)
        uri.scheme?.lowercase() in listOf("https", "http") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    fun source(url: String): String {
        val host = Uri.parse(url).host.orEmpty().lowercase().removePrefix("www.")
        return when {
            host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") -> "YouTube"
            host == "x.com" || host == "twitter.com" || host.endsWith(".twitter.com") || host.endsWith(".x.com") -> "X / Twitter"
            else -> host
        }
    }
}
