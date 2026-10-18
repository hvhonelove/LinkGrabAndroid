package com.example.linkgrab.core

import java.net.URI

object LinkParser {
    private val urlPattern = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private val tikTokVideo = Regex("^(/@[^/]+/video/\\d+)(?:/|$)", RegexOption.IGNORE_CASE)
    private val vkVideo = Regex("(?:^|/)video(-?\\d+_\\d+)(?:/|$)", RegexOption.IGNORE_CASE)

    fun extract(text: String): String {
        val url = urlPattern.find(text)?.value?.trimEnd('.', ',', ')', ']', '!', ';') ?: text.trim()
        return normalize(url)
    }

    /** Strip tracking from public TikTok video URLs, but preserve VK access keys and short redirects. */
    fun normalize(url: String): String = runCatching {
        val uri = URI(url)
        val host = uri.host.orEmpty().lowercase()
        if (host in setOf("www.tiktok.com", "tiktok.com", "m.tiktok.com")) {
            val match = tikTokVideo.find(uri.rawPath.orEmpty())
            if (match != null) return@runCatching "https://www.tiktok.com${match.groupValues[1]}"
        }
        if (host in setOf("vkvideo.ru", "www.vkvideo.ru", "vk.com", "m.vk.com")) {
            val match = vkVideo.find(uri.rawPath.orEmpty())
            if (match != null) {
                val query = uri.rawQuery?.let { "?$it" }.orEmpty()
                return@runCatching "https://vk.com/video${match.groupValues[1]}$query"
            }
        }
        url
    }.getOrDefault(url)

    fun isValid(url: String) = runCatching {
        val uri = URI(url)
        uri.scheme?.lowercase() in listOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)

    private fun domain(host: String, root: String) = host == root || host.endsWith(".$root")
    private fun host(url: String) = runCatching { URI(url).host.orEmpty().lowercase() }.getOrDefault("")

    fun isTikTok(url: String): Boolean = domain(host(url), "tiktok.com")
    fun isVkVideo(url: String): Boolean =
        domain(host(url), "vk.com") || domain(host(url), "vkvideo.ru") || domain(host(url), "vk.ru")

    fun source(url: String): String {
        val host = host(url)
        return when {
            domain(host, "youtube.com") || host == "youtu.be" -> "YouTube"
            domain(host, "x.com") || domain(host, "twitter.com") -> "X / Twitter"
            isTikTok(url) -> "TikTok"
            isVkVideo(url) -> "VK Видео"
            else -> host.removePrefix("www.")
        }
    }
}
