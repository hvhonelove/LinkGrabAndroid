package com.example.linkgrab.core

import java.net.URI

object LinkParser {
    fun extract(text: String): String = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
        .find(text)?.value?.trimEnd('.', ',', ')', ']', '!', ';') ?: text.trim()
    fun isValid(url: String) = runCatching {
        val uri = URI(url)
        uri.scheme?.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)
    fun source(url: String): String {
        val host = runCatching { URI(url).host.orEmpty().lowercase().removePrefix("www.") }.getOrDefault("")
        return when {
            host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") -> "YouTube"
            host == "x.com" || host == "twitter.com" || host.endsWith(".x.com") || host.endsWith(".twitter.com") -> "X / Twitter"
            else -> host
        }
    }
}
