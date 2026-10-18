package com.example.linkgrab.core

/** yt-dlp selectors independent from Android for fast JVM regression tests. */
object DownloadFormatSelector {
    fun video(url: String, quality: Int): String {
        val height = if (quality > 0) "[height<=?${quality.coerceAtMost(4320)}]" else ""
        if (!LinkParser.isTikTok(url)) return "bv*$height+ba/b$height"

        // TikTok's download_addr / download formats often contain a watermark.
        // Prefer the clean play_addr stream; never fall back to a known-watermarked format.
        val clean = "[format_note!*=?watermarked][format_id!^=?download]"
        return "b$height[format_id^=play_addr]$clean/" +
            "b$height$clean/" +
            "bv*$height$clean+ba/" +
            "bv*$height$clean"
    }
}
