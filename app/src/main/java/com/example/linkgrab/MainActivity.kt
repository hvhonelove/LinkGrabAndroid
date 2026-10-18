package com.example.linkgrab

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var urlField: EditText
    private lateinit var qualityField: Spinner
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var downloadButton: Button
    private lateinit var cancelButton: Button

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadService.ACTION_STATUS) return
            updateUi(
                intent.getStringExtra(DownloadService.EXTRA_STATUS).orEmpty(),
                intent.getIntExtra(DownloadService.EXTRA_PROGRESS, -1),
                intent.getBooleanExtra(DownloadService.EXTRA_BUSY, false)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(13, 18, 31)
        window.navigationBarColor = Color.rgb(13, 18, 31)
        val fg = Color.WHITE
        val bg = Color.rgb(13, 18, 31)
        val muted = Color.rgb(177, 188, 208)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(23), dp(32), dp(23), dp(32))
            setBackgroundColor(bg)
        }
        fun title(text: String, size: Float, color: Int = fg) {
            content.addView(TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                setPadding(0, dp(6), 0, dp(10))
            })
        }
        fun button(label: String, click: () -> Unit): Button {
            val b = Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener { click() }
            }
            content.addView(b, LinearLayout.LayoutParams(-1, dp(54)))
            return b
        }
        title("↧  LinkGrab", 32f)
        title("Скачивай видео из X / Twitter и YouTube по ссылке", 14f, muted)
        title("ССЫЛКА", 12f, muted)

        urlField = EditText(this).apply {
            hint = "https://x.com/... или youtube.com/..."
            setHintTextColor(muted)
            setTextColor(fg)
            setSingleLine(false)
            minLines = 2
            maxLines = 4
            setText("https://x.com/higgsfield_ai/status/2111509918490755394")
        }
        content.addView(urlField, LinearLayout.LayoutParams(-1, -2))
        button("Вставить из буфера") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val value = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            if (value.isBlank()) Toast.makeText(this, "Буфер пуст", Toast.LENGTH_SHORT).show()
            else urlField.setText(extractUrl(value))
        }
        title("КАЧЕСТВО", 12f, muted)
        qualityField = Spinner(this).apply {
            val names = listOf("Максимальное", "1080p", "720p")
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
        }
        content.addView(qualityField, LinearLayout.LayoutParams(-1, dp(56)))
        downloadButton = button("СКАЧАТЬ ВИДЕО") {
            val url = extractUrl(urlField.text.toString())
            val parsed = Uri.parse(url)
            if (parsed.scheme !in listOf("http", "https") || parsed.host.isNullOrBlank()) {
                urlField.error = "Вставь корректную ссылку"
            } else {
                val command = Intent(this, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_DOWNLOAD
                    putExtra(DownloadService.EXTRA_URL, url)
                    putExtra(DownloadService.EXTRA_QUALITY, qualityField.selectedItemPosition)
                }
                startWorker(command)
            }
        }
        cancelButton = button("Отменить") {
            startService(Intent(this, DownloadService::class.java).apply {
                action = DownloadService.ACTION_CANCEL
            })
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        content.addView(progress, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(20) })
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(15))
        }
        content.addView(status)
        button("Обновить движок yt-dlp") {
            startWorker(Intent(this, DownloadService::class.java).apply {
                action = DownloadService.ACTION_UPDATE
            })
        }
        title("Файлы сохраняются в Фильмы → LinkGrab. Можно использовать «Поделиться» в X или YouTube. Некоторые ролики требуют входа на сайт.", 12f, muted)
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        })
        receiveLink(intent)
        updateUi(DownloadService.latestStatus, DownloadService.latestProgress, DownloadService.active)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(DownloadService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
        updateUi(DownloadService.latestStatus, DownloadService.latestProgress, DownloadService.active)
    }

    override fun onStop() {
        unregisterReceiver(receiver)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveLink(intent)
    }

    private fun receiveLink(intent: Intent?) {
        val shared = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return
        urlField.setText(extractUrl(shared))
    }

    private fun extractUrl(text: String): String =
        Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE).find(text)?.value
            ?.trimEnd('.', ',', ')', ']') ?: text.trim()

    private fun startWorker(intent: Intent) {
        if (DownloadService.active) {
            Toast.makeText(this, "Загрузка уже идёт", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        updateUi("Подготовка…", -1, true)
    }

    private fun updateUi(message: String, percentage: Int, busy: Boolean) {
        if (!::status.isInitialized) return
        status.text = message.ifBlank { "Вставь ссылку и нажми «Скачать видео»" }
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        progress.isIndeterminate = percentage < 0
        if (percentage >= 0) progress.progress = percentage
        downloadButton.isEnabled = !busy
        cancelButton.isEnabled = busy
    }

    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
}
