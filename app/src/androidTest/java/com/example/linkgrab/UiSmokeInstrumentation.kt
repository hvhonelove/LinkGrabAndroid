package com.example.linkgrab

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import java.io.File

/** Runs against actual Android views; fixtures exist only in the test APK. */
class UiSmokeInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        var screen: Activity? = null
        try {
            check(LinkParser.extract("Смотри https://youtu.be/abc123).") == "https://youtu.be/abc123")
            check(LinkParser.isValid("https://youtube.com/watch?v=abc"))
            check(!LinkParser.isValid("javascript:alert(1)"))
            check(LinkParser.source("https://youtube.com.evil.example/test") != "YouTube")
            targetContext.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit().clear().commit()
            DownloadHistory.clear(targetContext)
            screen = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val activity = screen
            idle()
            screenshot("01-home")
            click(activity, "Вставить из буфера", before = {
                (targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("link", "Видео https://youtu.be/abc123"))
            })
            runOnMainSync {
                check((find(activity.window.decorView) { it is EditText } as EditText).text.toString() == "https://youtu.be/abc123")
            }
            click(activity, "Качество: 720p")
            check(targetContext.getSharedPreferences("preferences", Context.MODE_PRIVATE).getInt("quality", -1) == 2)
            click(activity, "Очистить ссылку")
            click(activity, "Скачать видео")
            runOnMainSync { check((find(activity.window.decorView) { it is EditText } as EditText).error != null) }
            click(activity, "Загрузки")
            screenshot("02-library-empty")
            click(activity, "Настройки")
            screenshot("03-settings")
            click(activity, "Главная")
            targetContext.sendBroadcast(Intent(DownloadService.ACTION_STATUS).setPackage(targetContext.packageName)
                .putExtra(DownloadService.EXTRA_STATUS, "Загрузка: 42% · осталось 12 с")
                .putExtra(DownloadService.EXTRA_PROGRESS, 42).putExtra(DownloadService.EXTRA_BUSY, true)
                .putExtra(DownloadService.EXTRA_STATE, "working"))
            idle()
            runOnMainSync { check(!findDescription(activity, "Скачать видео").isEnabled) }
            runOnMainSync {
                (find(activity.window.decorView) { it is android.widget.ScrollView } as android.widget.ScrollView)
                    .fullScroll(View.FOCUS_DOWN)
            }
            idle()
            screenshot("04-progress")
            targetContext.sendBroadcast(Intent(DownloadService.ACTION_STATUS).setPackage(targetContext.packageName)
                .putExtra(DownloadService.EXTRA_STATUS, "Проверка завершена")
                .putExtra(DownloadService.EXTRA_PROGRESS, 100).putExtra(DownloadService.EXTRA_BUSY, false)
                .putExtra(DownloadService.EXTRA_STATE, "success"))
            idle()
            DownloadHistory.add(targetContext, SavedVideo("content://media/external/video/media/99999999",
                "Вечерний город — тестовая запись", "YouTube", 24500000, System.currentTimeMillis(), 2))
            check(DownloadHistory.read(targetContext).single().bytes == 24500000L)
            click(activity, "Загрузки")
            screenshot("05-library-fixture")
            click(activity, "Открыть") // Missing media should show feedback, not crash.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            idle()
            screenshot("06-landscape")
            DownloadHistory.clear(targetContext)
            runOnMainSync { activity.finish() }
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "UI_SMOKE_OK: parser, clipboard, validation, quality, navigation, progress, history, missing media, rotation\n") })
        } catch (error: Throwable) {
            runCatching { screenshot("failure") }
            DownloadHistory.clear(targetContext)
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "UI_SMOKE_FAILED: ${error.stackTraceToString()}\n") })
        }
    }

    private fun idle() { waitForIdleSync(); Thread.sleep(400); waitForIdleSync() }
    private fun click(activity: Activity, description: String, before: () -> Unit = {}) {
        runOnMainSync { before(); check(findDescription(activity, description).performClick()) }
        idle()
    }
    private fun findDescription(activity: Activity, description: String): View =
        find(activity.window.decorView) { it.contentDescription?.toString() == description }
            ?: error("Missing control: $description")
    private fun find(view: View, predicate: (View) -> Boolean): View? {
        if (predicate(view)) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            find(view.getChildAt(index), predicate)?.let { return it }
        }
        return null
    }
    private fun screenshot(name: String) {
        val folder = File(targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
