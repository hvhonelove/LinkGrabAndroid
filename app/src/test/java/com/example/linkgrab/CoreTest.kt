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
