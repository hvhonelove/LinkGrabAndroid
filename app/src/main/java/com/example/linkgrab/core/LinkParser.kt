package com.example.linkgrab.core

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object LinkParser {
    fun extract(text: String): String = normalize(Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
        .find(text)?.value?.trimEnd('.', ',', ')', ']', '!', ';') ?: text.trim())

    /** Only canonicalize known public video routes; signed CDN URLs remain byte-for-byte intact. */
    fun normalize(url: String): String = runCatching {
        val uri = URI(url)
        if (!isValid(url)) return@runCatching url
        val host = uri.host.lowercase().removePrefix("www.")
        val path = uri.rawPath.orEmpty()
        if (isTikTok(url)) {
            val video = Regex("^/(?:@[^/]+/video|share/video|embed(?:/v2)?)/(\\d+)/?$").matchEntire(path)
            if (video != null) {
                val canonicalPath = if (path.startsWith("/@")) path.trimEnd('/') else "/@/video/${video.groupValues[1]}"
                return@runCatching "https://www.tiktok.com$canonicalPath"
            }
            // Short URLs need their redirect token and are resolved by yt-dlp on-device.
            if (host in setOf("vm.tiktok.com", "vt.tiktok.com") || path.startsWith("/t/"))
                return@runCatching URI("https", null, if (host == "tiktok.com") "www.tiktok.com" else host,
                    uri.port, uri.path, null, null).toASCIIString()
        }
        if (isVk(url)) {
            val query = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size != 2) null else URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()) to
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
            }.toMap()
            val nested = query["z"].orEmpty().ifBlank { query["w"].orEmpty() }
            val direct = Regex("^/(video|clip)(-?\\d+_\\d+)/?$").matchEntire(path)
            val wrapped = Regex("^(video|clip)(-?\\d+_\\d+)(?:/.*)?$").matchEntire(nested)
            val video = direct ?: wrapped
            if (video != null) {
                val destination = if (host == "vk.ru" || host.endsWith(".vk.ru")) "vk.com"
                    else host.removePrefix("m.").removePrefix("new.")
                return@runCatching "https://$destination/${video.groupValues[1]}${video.groupValues[2]}${uri.rawQuery?.let { "?$it" }.orEmpty()}"
            }
            // Preserve embedded oid/id/hash and access keys; never turn a restricted link into a public one.
            if (host == "vk.ru" || host.endsWith(".vk.ru"))
                return@runCatching "https://vk.com${uri.rawPath.orEmpty()}${uri.rawQuery?.let { "?$it" }.orEmpty()}${uri.rawFragment?.let { "#$it" }.orEmpty()}"
        }
        url
    }.getOrDefault(url)

    fun isTikTok(url: String) = hostMatches(url, "tiktok.com") || hostMatches(url, "tiktokv.com")
    fun isVk(url: String) = listOf("vk.com", "vk.ru", "vkvideo.ru").any { hostMatches(url, it) }
    private fun hostMatches(url: String, domain: String): Boolean {
        val host = runCatching { URI(url).host.orEmpty().lowercase() }.getOrDefault("")
        return host == domain || host.endsWith(".$domain")
    }
    fun isValid(url: String) = runCatching {
        val uri = URI(url)
        uri.scheme?.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)
    fun source(url: String): String {
        val host = runCatching { URI(url).host.orEmpty().lowercase().removePrefix("www.") }.getOrDefault("")
        return when {
            isTikTok(url) -> "TikTok"
            isVk(url) -> "VK Видео"
            host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") -> "YouTube"
            host == "x.com" || host == "twitter.com" || host.endsWith(".x.com") || host.endsWith(".twitter.com") -> "X / Twitter"
            else -> host
        }
    }
}
