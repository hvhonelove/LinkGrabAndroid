package com.example.linkgrab

import com.example.linkgrab.core.*
import com.example.linkgrab.domain.*
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test fun urls() {
        assertEquals("https://youtu.be/abc", LinkParser.extract("Смотри https://youtu.be/abc)."))
        assertTrue(LinkParser.isValid("https://x.com/user/status/12"))
        assertFalse(LinkParser.isValid("javascript:alert(1)"))
        assertFalse(LinkParser.isValid("https://user:secret@example.com/a"))
        assertEquals("youtube.com.evil.example", LinkParser.source("https://youtube.com.evil.example/v"))
        assertEquals("YouTube", LinkParser.source("https://m.youtube.com/watch?v=a"))
    }
    @Test fun tiktokShareLinksAndSource() {
        val shared = "https://www.tiktok.com/@morgenshtern_israel/video/7696573226729622804?_r=1&share_item_id=7696573226729622804&share_app_id=1233&link_reflow_popup_iteration_sharer=%7B%22click_empty_to_play%22%3A1%7D"
        val expected = "https://www.tiktok.com/@morgenshtern_israel/video/7696573226729622804"
        assertEquals(expected, LinkParser.extract("Смотри это видео: $shared"))
        assertEquals(expected, LinkParser.normalize(shared))
        assertEquals("TikTok", LinkParser.source(shared))
        assertTrue(LinkParser.isValid(expected))
        assertEquals("https://vt.tiktok.com/ZS123abc/", LinkParser.extract("https://vt.tiktok.com/ZS123abc/"))
        assertEquals("TikTok", LinkParser.source("https://vm.tiktok.com/abcXYZ/"))
        assertNotEquals("TikTok", LinkParser.source("https://tiktok.com.evil.example/video/1"))
    }

    @Test fun vkVideoLinks() {
        assertEquals("VK Видео", LinkParser.source("https://vkvideo.ru/video-127553155_456242961"))
        assertEquals("VK Видео", LinkParser.source("https://m.vk.com/video-127553155_456242961"))
        assertEquals("https://vk.com/video-212131631_456240209",
            LinkParser.extract("https://vkvideo.ru/playlist/-212131631_-2/video-212131631_456240209"))
        assertEquals("https://vk.com/video-127553155_456242961?list=abc",
            LinkParser.normalize("https://vkvideo.ru/video-127553155_456242961?list=abc"))
        assertNotEquals("VK Видео", LinkParser.source("https://vkvideo.ru.evil.example/video-1_2"))
    }

    @Test fun watermarkFreeFormatPreference() {
        val tiktok = DownloadFormatSelector.video("https://www.tiktok.com/@someone/video/123456", 720)
        assertTrue(tiktok.contains("[format_id^=play_addr]"))
        assertTrue(tiktok.contains("[format_note!*=?watermarked]"))
        assertTrue(tiktok.contains("[format_id!^=?download]"))
        assertTrue(tiktok.contains("[height<=?720]"))
        assertEquals("bv*[height<=?1080]+ba/b[height<=?1080]",
            DownloadFormatSelector.video("https://vkvideo.ru/video-1_2", 1080))
        assertEquals("bv*+ba/b", DownloadFormatSelector.video("https://x.com/u/status/1", 0))
    }

    @Test fun qualityAndEstimate() {
        val info = VideoMetadata("https://example.com/v", "Title", null, 20, "Author", "Site", listOf(
            MediaFormat("video", 720, "mp4", 10_000, true, false), MediaFormat("audio", 0, "m4a", 2_000, false, true)))
        assertEquals(listOf(0, 720), info.qualities)
        assertEquals(12_000L, info.estimate(DownloadOptions()))
        assertEquals(2_000L, info.estimate(DownloadOptions(kind = MediaKind.AUDIO)))
        assertEquals(0L, info.estimate(DownloadOptions(480)))
    }
    @Test fun filenamesAndErrors() {
        assertEquals("bad_name__", safeFileName("bad/name:*"))
        assertEquals("LinkGrab", safeFileName("..."))
        assertTrue(ErrorMapper.message(IllegalStateException("HTTP Error 403")).contains("ограничил"))
        assertEquals("1:03", durationLabel(63))
    }
}
