package com.example.linkgrab.core

/**
 * Prefer direct clean TikTok playback instead of the download endpoint which may include a
 * watermark. Formats vary by extractor version: some use play, some use play_addr.
 */
object DownloadFormatSelector {
    private fun heightLimit(quality: Int): String =
        if (quality > 0) "[height<=?${quality.coerceAtMost(4320)}]" else ""

    private const val CLEAN =
        "[format_note!*=?watermarked][format_id!^=?download]"

    fun video(url: String, quality: Int, alternate: Boolean = false): String {
        val height = heightLimit(quality)
        if (!LinkParser.isTikTok(url)) return "bv*$height+ba/b$height"
        val play = "b$height[format_id^=play_addr]$CLEAN/" +
            "b$height[format_id=play]$CLEAN"
        val other = "b$height$CLEAN/bv*$height$CLEAN+ba"
        return if (alternate) "$other/$play" else "$play/$other"
    }
}
