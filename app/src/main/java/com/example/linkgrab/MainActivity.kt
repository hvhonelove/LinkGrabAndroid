package com.example.linkgrab

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.linkgrab.data.db.DownloadEntity
import com.example.linkgrab.domain.MediaKind
import com.example.linkgrab.feature.downloads.FileAction
import com.example.linkgrab.feature.home.HomeViewModel
import com.example.linkgrab.ui.LinkGrabApp
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val graph get() = (application as LinkGrabApplication).container
    private val home: HomeViewModel by viewModels {
        viewModelFactory { initializer { HomeViewModel(graph, createSavedStateHandle()) } }
    }
    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) receive(intent)
        setContent { LinkGrabApp(graph, home, ::fileAction, ::notifications, ::openUrl) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    private fun receive(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT)?.let(home::receiveShare)
    }
    private fun notifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else runCatching { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }
    }
    private fun openUrl(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { toast("Не найден браузер") } }
    private fun fileAction(item: DownloadEntity, action: FileAction) {
        when (action) {
            FileAction.COPY_LINK -> {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Ссылка", item.url))
                toast("Ссылка скопирована")
            }
            FileAction.FOLDER -> {
                val path = if (item.kind == MediaKind.AUDIO) "Music/LinkGrab" else "Movies/LinkGrab"
                val directory = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:$path")
                runCatching { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(directory, DocumentsContract.Document.MIME_TYPE_DIR)) }
                    .onFailure { toast("Открой «Мои файлы» → $path") }
            }
            FileAction.OPEN, FileAction.SHARE -> lifecycleScope.launch {
                val uri = item.uri?.let(Uri::parse) ?: return@launch
                val exists = withContext(Dispatchers.IO) {
                    runCatching { contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)
                }
                if (!exists) { toast("Файл удалён или недоступен. Можно скачать его ещё раз."); return@launch }
                val command = Intent(if (action == FileAction.SHARE) Intent.ACTION_SEND else Intent.ACTION_VIEW).apply {
                    if (action == FileAction.SHARE) { type = item.mime; putExtra(Intent.EXTRA_STREAM, uri) }
                    else setDataAndType(uri, item.mime)
                    clipData = ClipData.newRawUri("Медиа", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { startActivity(Intent.createChooser(command, if (action == FileAction.SHARE) "Поделиться" else "Открыть файл")) }
                    .onFailure { toast("Не найдено приложение для этого формата") }
            }
        }
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
