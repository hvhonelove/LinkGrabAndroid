package com.example.linkgrab.data.engine

import android.content.Context
import android.util.AtomicFile
import com.example.linkgrab.BuildConfig
import com.example.linkgrab.R
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.zip.ZipFile

/** Upgrade the wrapper's persisted Python zip on app update, preserving newer in-app updates. */
internal class BundledYtDlp(private val context: Context) {
    private val binary = File(context.noBackupFilesDir,
        "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}/${YoutubeDL.ytdlpBin}")
    private var prepared = false

    fun prepare(): String {
        if (prepared) return version() ?: BuildConfig.BUNDLED_YTDLP_VERSION
        val installed = version()
        if (installed == null || installed.take(10) < BuildConfig.BUNDLED_YTDLP_VERSION) {
            check(binary.parentFile!!.isDirectory || binary.parentFile!!.mkdirs()) { "Не удалось обновить встроенный загрузчик" }
            val atomic = AtomicFile(binary)
            val output = atomic.startWrite()
            try {
                context.resources.openRawResource(R.raw.ytdlp).use { it.copyTo(output) }
                atomic.finishWrite(output)
            } catch (error: Exception) {
                atomic.failWrite(output)
                throw error
            }
        }
        prepared = true
        return version() ?: error("Не удалось проверить встроенный загрузчик")
    }

    private fun version(): String? = runCatching {
        ZipFile(binary).use { zip ->
            val entry = zip.getEntry("yt_dlp/version.py") ?: return@use null
            val text = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            Regex("__version__\\s*=\\s*['\"](\\d{4}\\.\\d{2}\\.\\d{2}[^'\"]*)['\"]")
                .find(text)?.groupValues?.get(1)
        }
    }.getOrNull()
}
