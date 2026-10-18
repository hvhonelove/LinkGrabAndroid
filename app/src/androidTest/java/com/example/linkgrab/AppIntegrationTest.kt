package com.example.linkgrab

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.linkgrab.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val graph get() = (context.applicationContext as LinkGrabApplication).container
    private var scenario: ActivityScenario<MainActivity>? = null
    @Before fun setup() = runBlocking {
        graph.ready.await()
        withContext(Dispatchers.IO) { graph.database.clearAllTables() }
        graph.settings.flag("onboarded", false)
        graph.settings.flag("notificationAsked", true)
        graph.settings.text("theme", "LIGHT")
        graph.settings.flag("autoUpdate", false)
    }
    @After fun tearDown() { scenario?.close() }
    @Test fun realEngineToMediaStoreAndCompose() {
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("fixture.mp4").use { it.readBytes() }
        FixtureServer(bytes).use { server ->
            scenario = ActivityScenario.launch(MainActivity::class.java)
            compose.waitUntil(20000) { compose.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty() }
            screenshot("01-onboarding")
            repeat(3) { compose.onNodeWithTag("onboarding-next").performClick(); compose.waitForIdle() }
            compose.waitUntil(10000) { compose.onAllNodesWithTag("home").fetchSemanticsNodes().isNotEmpty() }
            screenshot("02-home-light")
            compose.onNodeWithTag("link-input").performTextInput(server.url)
            compose.onNodeWithTag("analyze").performScrollTo().performClick()
            compose.waitUntil(90000) { compose.onAllNodesWithTag("preview-title").fetchSemanticsNodes().isNotEmpty() }
            screenshot("03-preview")
            compose.onNodeWithTag("download-options").performClick()
            compose.onNodeWithTag("enqueue").performClick()
            compose.waitUntil(90000) { runBlocking { graph.downloads.items.first().any { it.status == TaskStatus.COMPLETED } } }
            val item = runBlocking { graph.downloads.items.first().first { it.status == TaskStatus.COMPLETED } }
            assertNotNull(item.uri)
            val downloaded = context.contentResolver.openInputStream(Uri.parse(item.uri))!!.use { it.readBytes() }
            assertArrayEquals(bytes, downloaded)
            compose.onNodeWithText("Готовые").performClick()
            screenshot("04-history")
            compose.onNodeWithTag("task-${item.id}").performClick()
            compose.onNodeWithTag("rename").performScrollTo().performClick()
            compose.onNodeWithTag("rename-input").performTextReplacement("Saved integration video")
            compose.onNodeWithText("Сохранить").performClick()
            compose.waitUntil(10000) { runBlocking { graph.downloads.dao.get(item.id)?.title == "Saved integration video" } }
            scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithTag("nav-2").performClick()
            screenshot("05-settings-light")
            compose.onNodeWithText("Тёмная").performClick()
            compose.waitForIdle()
            screenshot("06-settings-dark")
            scenario!!.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitForIdle()
            screenshot("07-landscape")
            runBlocking {
                graph.downloads.remove(item.id, true)
                assertNull(graph.downloads.dao.get(item.id))
                assertFalse(graph.storage.isPublished(item.uri!!))
            }
            // Exercise audio extraction through the same bundled FFmpeg runtime.
            runBlocking {
                val info = graph.engine.analyze(server.url)
                val id = graph.downloads.enqueue(info, DownloadOptions(kind = MediaKind.AUDIO, format = ContainerFormat.MP3))
                val notifications = com.example.linkgrab.service.DownloadNotifications(context)
                com.example.linkgrab.service.QueueProcessor(graph, notifications).runOne(graph.downloads.dao.get(id)!!)
                val audio = graph.downloads.dao.get(id)!!
                assertEquals(audio.error, TaskStatus.COMPLETED, audio.status)
                assertEquals("audio/mpeg", audio.mime)
                graph.downloads.remove(id, true)
            }
        }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
