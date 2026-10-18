package com.example.linkgrab

import com.example.linkgrab.core.DownloadPolicy
import com.example.linkgrab.core.LinkParser
import org.junit.Assert.*
import org.junit.Test

class PlatformLinksTest {
    @Test fun tikTokAndroidShareHasOneCanonicalVideoUrl() {
        val canonical = "https://www.tiktok.com/@morgenshtern_israel/video/7696573226729622804"
        val shared = "$canonical?_r=1&u_code=test&preview_pb=0&sharer_language=ru&share_item_id=7696573226729622804" +
            "&utm_source=copy&share_link_id=test&link_reflow_popup_iteration_sharer=%7B%22click_empty_to_play%22%3A1%7D" +
            "&tracking=" + "x".repeat(4000)
        assertEquals(canonical, LinkParser.extract("Посмотри это видео: $shared"))
        assertEquals(canonical, LinkParser.normalize(canonical))
        assertEquals("TikTok", LinkParser.source(shared))
        assertEquals("https://www.tiktok.com/@/video/12345", LinkParser.normalize("https://tiktok.com/share/video/12345?utm_source=copy"))
        assertEquals("https://www.tiktok.com/@/video/12345", LinkParser.normalize("https://www.tiktok.com/embed/v2/12345"))
    }

    @Test fun shortTikTokTokensSurviveWithoutTracking() {
        assertEquals("https://vm.tiktok.com/ZM123/", LinkParser.normalize("https://vm.tiktok.com/ZM123/?share_app_id=1233"))
        assertEquals("https://vt.tiktok.com/ZS123", LinkParser.normalize("https://vt.tiktok.com/ZS123?utm_campaign=client_share"))
        assertEquals("https://www.tiktok.com/t/ZT123/", LinkParser.normalize("https://www.tiktok.com/t/ZT123/?k=1"))
    }

    @Test fun vkVideoAndEncodedPlayerLinksPreserveAccessParameters() {
        assertEquals("VK Видео", LinkParser.source("https://vkvideo.ru/video-123_456"))
        assertEquals("VK Видео", LinkParser.source("https://m.vk.ru/clip-123_456"))
        assertEquals("https://vk.com/video-123_456", LinkParser.normalize("https://m.vk.ru/video-123_456"))
        assertEquals("https://vkvideo.ru/video-123_456?list=abc&access_key=secret", LinkParser.normalize("https://vkvideo.ru/video-123_456?list=abc&access_key=secret"))
        val query = "z=video-123_456%2Fclub123&list=ln-abc"
        assertEquals("https://vk.com/video-123_456?$query", LinkParser.normalize("https://vk.com/videos-123?$query"))
        val embed = "https://vkvideo.ru/video_ext.php?oid=-123&id=456&hash=abc"
        assertEquals(embed, LinkParser.normalize(embed))
    }

    @Test fun unrelatedAndSignedLinksAreNotRewritten() {
        val signed = "https://cdn.example.com/media.mp4?token=a%2Bb%3D&expires=123#time=1"
        assertEquals(signed, LinkParser.normalize(signed))
        assertFalse(LinkParser.isTikTok("https://tiktok.com.evil.example/@user/video/123"))
        assertFalse(LinkParser.isVk("https://vkvideo.ru.evil.example/video-1_2"))
        assertEquals("https://user:secret@vk.ru/video-1_2", LinkParser.normalize("https://user:secret@vk.ru/video-1_2"))
    }

    @Test fun markedTikTokDownloadsAreNeverFallbackCandidates() {
        assertFalse(DownloadPolicy.isAllowedTikTokFormat("download", ""))
        assertFalse(DownloadPolicy.isAllowedTikTokFormat("download_addr-1", "Download video"))
        assertFalse(DownloadPolicy.isAllowedTikTokFormat("other", "Download video, watermarked"))
        assertTrue(DownloadPolicy.isAllowedTikTokFormat("play", ""))
        assertTrue(DownloadPolicy.isAllowedTikTokFormat("h264_720p_1000-0", ""))
        assertEquals("bv*+ba/b", DownloadPolicy.selector("https://youtube.com/watch?v=a", 0, false))
        assertTrue(DownloadPolicy.selector("https://vt.tiktok.com/ZS123", 720, false).contains("[format_note!*=?watermark]"))
        assertTrue(DownloadPolicy.selector("https://vt.tiktok.com/ZS123", 720, false).contains("[height<=720]"))
    }
}
