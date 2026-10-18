package com.example.linkgrab.core

/** Shared by analysis and download, so the preview cannot advertise a watermarked fallback. */
object DownloadPolicy {
    private const val TIKTOK_FILTER = "[format_note!*=?watermark][format_id!^=download]"
    fun isAllowedTikTokFormat(id: String, note: String): Boolean =
        !id.startsWith("download", ignoreCase = true) && !note.contains("watermark", ignoreCase = true)

    fun selector(url: String, quality: Int, audio: Boolean): String {
        val filter = if (LinkParser.isTikTok(url)) TIKTOK_FILTER else ""
        if (audio) return "ba$filter/b$filter"
        val limit = if (quality > 0) "[height<=$quality]" else ""
        return "bv*$limit$filter+ba$filter/b$limit$filter"
    }
}
