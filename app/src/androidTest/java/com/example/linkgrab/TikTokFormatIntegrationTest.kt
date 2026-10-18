package com.example.linkgrab

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.linkgrab.core.DownloadPolicy
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TikTokFormatIntegrationTest {
    @Test fun nativeSelectorRejectsWatermarkedHigherResolutionAndFallback() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<LinkGrabApplication>()
        app.container.engine.checkVersion()
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("fixture.mp4").use { it.readBytes() }
        FixtureServer(bytes).use { server ->
            val source = "https://www.tiktok.com/@creator/video/123456"
            val input = File(app.cacheDir, "tiktok-format-test.json")
            fun format(id: String, height: Int, note: String) = JSONObject().apply {
                put("format_id", id); put("url", server.url); put("height", height)
                put("width", height * 16 / 9); put("ext", "mp4"); put("vcodec", "h264"); put("acodec", "aac")
                put("format_note", note)
            }
            val watermarked = format("download", 2160, "watermarked")
            fun metadata(formats: JSONArray) = JSONObject().apply {
                put("id", "123456"); put("title", "Test media"); put("webpage_url", source)
                put("extractor", "TikTok"); put("extractor_key", "TikTok"); put("formats", formats)
            }
            fun request() = YoutubeDLRequest(emptyList<String>()).apply {
                addOption("--load-info-json", input.absolutePath)
                addOption("--skip-download"); addOption("--dump-single-json")
                addOption("-f", DownloadPolicy.selector(source, 0, false))
            }
            try {
                input.writeText(metadata(JSONArray().put(watermarked).put(format("play", 720, ""))).toString())
                val result = withContext(Dispatchers.IO) { YoutubeDL.execute(request(), "tiktok-format-ok", false, null).out }
                assertEquals("play", JSONObject(result).getString("format_id"))
                input.writeText(metadata(JSONArray().put(watermarked)).toString())
                val error = withContext(Dispatchers.IO) {
                    runCatching { YoutubeDL.execute(request(), "tiktok-format-reject", false, null) }.exceptionOrNull()
                }
                assertNotNull("Watermarked-only metadata must fail rather than download it", error)
                assertTrue(error!!.message.orEmpty().contains("Requested format"))
            } finally { input.delete() }
        }
    }
}
