package com.example.linkgrab

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val bg = Color.rgb(12, 17, 21)
    private val surface = Color.rgb(22, 29, 34)
    private val border = Color.rgb(42, 54, 61)
    private val ink = Color.rgb(241, 247, 244)
    private val muted = Color.rgb(158, 175, 179)
    private val mint = Color.rgb(166, 239, 204)
    private val orange = Color.rgb(255, 191, 137)
    private val prefs by lazy { getSharedPreferences("preferences", MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private lateinit var page: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var footer: LinearLayout
    private var tab = 0
    private var selectedQuality = 0
    private var draftUrl = ""
    private var urlField: EditText? = null
    private var sourceLabel: TextView? = null
    private var primaryAction: View? = null
    private var statusCard: LinearLayout? = null
    private var statusTitle: TextView? = null
    private var statusDetail: TextView? = null
    private var progress: ProgressBar? = null
    private var cancelAction: View? = null
    private var latestMessage = ""
    private var latestProgress = -1
    private var busy = false
    private var resultState = "idle"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadService.ACTION_STATUS) return
            val wasBusy = busy
            latestMessage = intent.getStringExtra(DownloadService.EXTRA_STATUS).orEmpty()
            latestProgress = intent.getIntExtra(DownloadService.EXTRA_PROGRESS, -1)
            busy = intent.getBooleanExtra(DownloadService.EXTRA_BUSY, false)
            resultState = intent.getStringExtra(DownloadService.EXTRA_STATE) ?: "idle"
            updateStatus()
            if (wasBusy && !busy) renderPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        selectedQuality = savedInstanceState?.getInt("quality")
            ?: prefs.getInt("quality", 0).coerceIn(0, 2)
        draftUrl = savedInstanceState?.getString("url").orEmpty()
        tab = savedInstanceState?.getInt("tab") ?: 0
        syncService()
        root = column().apply { setBackgroundColor(bg) }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }
        page = column().apply { setPadding(dp(22), dp(22), dp(22), dp(26)) }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        footer = column().apply { setPadding(dp(22), dp(10), dp(22), dp(10)) }
        root.addView(footer)
        nav = row().apply { setPadding(dp(10), dp(8), dp(10), dp(10)); setBackgroundColor(bg) }
        root.addView(View(this).apply { setBackgroundColor(border) }, LinearLayout.LayoutParams(-1, dp(1)))
        root.addView(nav)
        setContentView(root)
        if (savedInstanceState == null) receiveLink(intent)
        renderPage()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(DownloadService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
        syncService()
        renderPage()
    }

    override fun onStop() {
        unregisterReceiver(receiver)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("url", draftUrl)
        outState.putInt("quality", selectedQuality)
        outState.putInt("tab", tab)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveLink(intent)
        renderPage()
    }

    private fun syncService() {
        latestMessage = DownloadService.latestStatus
        latestProgress = DownloadService.latestProgress
        busy = DownloadService.active
        resultState = DownloadService.latestState
    }

    private fun renderPage() {
        urlField = null; sourceLabel = null; primaryAction = null
        statusCard = null; statusTitle = null; statusDetail = null; progress = null; cancelAction = null
        page.removeAllViews()
        footer.removeAllViews()
        footer.visibility = if (tab == 0) View.VISIBLE else View.GONE
        renderNav()
        when (tab) {
            1 -> renderLibrary()
            2 -> renderSettings()
            else -> renderHome()
        }
    }

    private fun header(kicker: String, title: String, subtitle: String) {
        val top = row()
        top.addView(text("LINKGRAB", 12f, mint, true).apply { letterSpacing = .16f },
            LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text(kicker, 11f, muted).apply { letterSpacing = .08f })
        page.addView(top)
        page.addView(text(title, 34f, ink, true), spaced(top = 24))
        page.addView(text(subtitle, 14f, muted).apply { setLineSpacing(dp(3).toFloat(), 1f) }, spaced(top = 8, bottom = 24))
    }

    private fun renderHome() {
        header("VIDEO DOWNLOADER", "Сохрани момент.", "Любимое видео — теперь у тебя.")
        val hero = column().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(37, 65, 57), Color.rgb(24, 39, 39))).apply { cornerRadius = dp(24).toFloat() }
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val heroTop = row()
        heroTop.addView(icon("download", mint, 36))
        heroTop.addView(text("ИЗ ЛЕНТЫ В ГАЛЕРЕЮ", 10f, mint, true).apply { letterSpacing = .1f },
            LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(12) })
        hero.addView(heroTop)
        hero.addView(text("Одна ссылка.\nИ видео твоё.", 25f, ink, true), spaced(top = 18))
        val platforms = row().apply { setPadding(0, dp(16), 0, 0) }
        listOf("YouTube", "X / Twitter", "+ другие").forEach { label ->
            platforms.addView(text(label, 11f, mint).apply {
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = shape(Color.rgb(42, 67, 60), 10)
            }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(7) })
        }
        hero.addView(platforms)
        page.addView(hero)

        section("Ссылка на видео", "01")
        val inputCard = column().apply {
            background = shape(surface, 18, border)
            setPadding(dp(14), dp(8), dp(10), dp(8))
        }
        val inputRow = row()
        val field = EditText(this).apply {
            id = View.generateViewId()
            hint = "Вставь ссылку сюда…"
            contentDescription = "Ссылка на видео"
            setHintTextColor(muted); setTextColor(ink); textSize = 15f
            background = null
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(dp(2), dp(12), dp(4), dp(12))
            setText(draftUrl)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard(); clearFocus(); true
                } else false
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    draftUrl = s.toString(); updateSource()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        urlField = field
        inputRow.addView(field, LinearLayout.LayoutParams(0, -2, 1f))
        inputRow.addView(iconAction("close", "Очистить ссылку") { field.setText("") })
        inputCard.addView(inputRow)
        sourceLabel = text("", 12f, muted).apply { setPadding(dp(2), 0, 0, dp(8)) }
        inputCard.addView(sourceLabel)
        page.addView(inputCard)
        page.addView(action("Вставить из буфера", "paste", false) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val value = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            if (value.isBlank()) toast("Буфер обмена пуст")
            else field.setText(LinkParser.extract(value))
        }, spaced(top = 10))
        updateSource()

        section("Качество видео", "02")
        val qualityRow = row()
        listOf("Лучшее" to "Оригинал", "1080p" to "Full HD", "720p" to "Меньше вес").forEachIndexed { index, label ->
            val selected = selectedQuality == index
            val choice = column().apply {
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(13), dp(4), dp(13))
                background = shape(if (selected) Color.rgb(32, 58, 47) else surface, 16, if (selected) mint else border)
                contentDescription = "Качество: ${label.first}"
                isSelected = selected
                addView(text(label.first, 16f, if (selected) mint else ink, true).apply { gravity = Gravity.CENTER })
                addView(text(label.second, 10f, if (selected) mint else muted).apply {
                    gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0)
                })
                setOnClickListener {
                    hideKeyboard()
                    selectedQuality = index
                    prefs.edit().putInt("quality", index).apply()
                    renderPage()
                }
                isFocusable = true
            }
            qualityRow.addView(choice, LinearLayout.LayoutParams(0, -2, 1f).apply { if (index < 2) rightMargin = dp(8) })
        }
        page.addView(qualityRow)
        page.addView(text("Максимум до выбранного разрешения", 11f, muted), spaced(top = 9, bottom = 20))
        primaryAction = action("Скачать видео", "download", true) { download() }
        footer.addView(primaryAction)
        renderStatus()
        val folder = row().apply { setPadding(dp(2), dp(18), 0, 0) }
        folder.addView(icon("folder", muted, 17))
        folder.addView(text("Сохраняем в Movies / LinkGrab", 11f, muted),
            LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        page.addView(folder)
        val saved = DownloadHistory.read(this)
        if (saved.isNotEmpty()) {
            section("Последнее видео", "")
            addVideo(saved.first())
        }
        updateStatus()
    }

    private fun renderStatus() {
        val card = column().apply { background = shape(surface, 18, border); setPadding(dp(18), dp(16), dp(18), dp(14)) }
        statusCard = card
        statusTitle = text("", 16f, mint, true)
        card.addView(statusTitle)
        statusDetail = text("", 12f, muted).apply { setLineSpacing(dp(3).toFloat(), 1f); setTextIsSelectable(true) }
        card.addView(statusDetail, spaced(top = 7))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progressTintList = ColorStateList.valueOf(mint)
            indeterminateTintList = ColorStateList.valueOf(mint)
            progressBackgroundTintList = ColorStateList.valueOf(border)
        }
        card.addView(progress, LinearLayout.LayoutParams(-1, dp(6)).apply { topMargin = dp(14); bottomMargin = dp(10) })
        cancelAction = action("Отменить", "close", false) {
            startService(Intent(this, DownloadService::class.java).setAction(DownloadService.ACTION_CANCEL))
        }
        card.addView(cancelAction)
        page.addView(card, spaced(top = 16))
    }

    private fun updateStatus() {
        primaryAction?.isEnabled = !busy
        primaryAction?.alpha = if (busy) .45f else 1f
        statusCard?.visibility = if (busy || latestMessage.isNotBlank()) View.VISIBLE else View.GONE
        statusTitle?.text = when {
            busy -> if (latestProgress in 0..100) "Загружаем · $latestProgress%" else "В работе…"
            resultState == "success" -> "Готово!"
            resultState == "cancelled" -> "Загрузка отменена"
            resultState == "error" -> "Не получилось скачать"
            else -> "Статус"
        }
        statusTitle?.setTextColor(if (resultState == "error") orange else mint)
        statusDetail?.text = latestMessage
        progress?.visibility = if (busy) View.VISIBLE else View.GONE
        progress?.isIndeterminate = latestProgress < 0
        if (latestProgress >= 0) progress?.setProgress(latestProgress, true)
        cancelAction?.visibility = if (busy) View.VISIBLE else View.GONE
    }

    private fun renderLibrary() {
        header("ТВОЯ КОЛЛЕКЦИЯ", "Загрузки", "Сохранённые моменты всегда рядом.")
        if (busy) renderStatus()
        val videos = DownloadHistory.read(this)
        val summary = row().apply {
            background = shape(Color.rgb(29, 48, 42), 18)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        summary.addView(icon("folder", mint, 30))
        val details = column().apply { setPadding(dp(14), 0, 0, 0) }
        details.addView(text("${videos.size} в истории", 19f, ink, true))
        details.addView(text(Formatter.formatFileSize(this, videos.sumOf { it.bytes }) + " · Movies / LinkGrab", 11f, muted), spaced(top = 5))
        summary.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(summary, spaced(bottom = 22))
        if (videos.isEmpty()) {
            val empty = column().apply {
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(38), dp(24), dp(38))
                background = shape(surface, 22, border)
            }
            empty.addView(icon("download", mint, 44))
            empty.addView(text("Здесь будут твои видео", 19f, ink, true).apply { gravity = Gravity.CENTER }, spaced(top = 20))
            empty.addView(text("Скопируй ссылку или отправь её\nв LinkGrab через «Поделиться».", 13f, muted).apply {
                gravity = Gravity.CENTER; setLineSpacing(dp(4).toFloat(), 1f)
            }, spaced(top = 10, bottom = 22))
            empty.addView(action("Скачать первое видео", "arrow", true) { tab = 0; renderPage() })
            page.addView(empty)
        } else videos.forEach { addVideo(it) }
        updateStatus()
    }

    private fun addVideo(video: SavedVideo) {
        val card = column().apply { background = shape(surface, 18, border); setPadding(dp(16), dp(16), dp(16), dp(10)) }
        val top = row()
        top.addView(icon("play", mint, 27))
        val detail = column().apply { setPadding(dp(12), 0, 0, 0) }
        detail.addView(text(video.title, 14f, ink, true).apply { maxLines = 3; ellipsize = android.text.TextUtils.TruncateAt.END })
        val date = SimpleDateFormat("d MMM · HH:mm", Locale("ru")).format(Date(video.savedAt))
        detail.addView(text("${Formatter.formatFileSize(this, video.bytes)} · $date", 11f, muted), spaced(top = 5))
        if (video.source.isNotBlank()) detail.addView(text(video.source, 11f, mint), spaced(top = 4))
        top.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(top)
        val actions = row().apply { setPadding(0, dp(12), 0, 0) }
        actions.addView(action("Открыть", "play", false) { openVideo(video, false) },
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
        actions.addView(iconAction("share", "Поделиться видео") { openVideo(video, true) })
        card.addView(actions)
        page.addView(card, spaced(bottom = 12))
    }

    private fun renderSettings() {
        header("ПОД ТЕБЯ", "Настройки", "Всё нужное. Ничего лишнего.")
        val storage = column().apply {
            background = shape(surface, 20, border); setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        storage.addView(icon("folder", mint, 28))
        storage.addView(text("Сразу в галерею", 20f, ink, true), spaced(top = 15))
        storage.addView(text("Видео сохраняются в Movies / LinkGrab. Открывай их в галерее или на вкладке «Загрузки».", 13f, muted), spaced(top = 8))
        page.addView(storage)
        section("Если сайт изменился", "ДВИЖОК")
        page.addView(text("Обновление загрузчика может помочь, если ссылка перестала скачиваться.", 13f, muted), spaced(bottom = 12))
        primaryAction = action("Обновить загрузчик", "refresh", true) {
            startWorker(Intent(this, DownloadService::class.java).setAction(DownloadService.ACTION_UPDATE))
        }
        page.addView(primaryAction)
        renderStatus()
        section("Порядок в приложении", "ИСТОРИЯ")
        page.addView(action("Очистить историю", "close", false) {
            AlertDialog.Builder(this).setTitle("Очистить историю?")
                .setMessage("Записи исчезнут из LinkGrab. Скачанные видео останутся в галерее.")
                .setNegativeButton("Оставить", null)
                .setPositiveButton("Очистить") { _, _ -> DownloadHistory.clear(this); renderPage(); toast("История очищена") }
                .show()
        })
        page.addView(text("LinkGrab ${BuildConfig.VERSION_NAME}\nНа базе yt-dlp и FFmpeg · GPL-3.0", 11f, muted).apply {
            gravity = Gravity.CENTER; setLineSpacing(dp(5).toFloat(), 1f)
        }, spaced(top = 30))
        updateStatus()
    }

    private fun renderNav() {
        nav.removeAllViews()
        listOf("Главная" to "home", "Загрузки" to "download", "Настройки" to "settings").forEachIndexed { index, item ->
            val selected = index == tab
            val button = column().apply {
                gravity = Gravity.CENTER
                minimumHeight = dp(60)
                setPadding(dp(6), dp(8), dp(6), dp(8))
                background = shape(if (selected) Color.rgb(29, 48, 42) else bg, 16)
                addView(icon(item.second, if (selected) mint else muted, 22))
                addView(text(item.first, 10f, if (selected) mint else muted, selected).apply {
                    gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0)
                })
                contentDescription = item.first
                isSelected = selected
                setOnClickListener { hideKeyboard(); tab = index; renderPage() }
                isFocusable = true
            }
            nav.addView(button, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        }
    }

    private fun updateSource() {
        val url = LinkParser.extract(draftUrl)
        sourceLabel?.text = if (draftUrl.isBlank()) "Можно вставить ссылку вместе с текстом" else
            if (LinkParser.isValid(url)) "Источник: ${LinkParser.source(url)}" else "Нужна ссылка, начинающаяся с https://"
    }

    private fun receiveLink(intent: Intent?) {
        val shared = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return
        draftUrl = LinkParser.extract(shared)
        tab = 0
    }

    private fun download() {
        val url = LinkParser.extract(draftUrl)
        if (!LinkParser.isValid(url)) {
            urlField?.error = "Вставь корректную ссылку на видео"
            urlField?.requestFocus()
            return
        }
        draftUrl = url; urlField?.setText(url)
        startWorker(Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_DOWNLOAD
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_QUALITY, selectedQuality)
        })
    }

    private fun startWorker(command: Intent) {
        if (busy || DownloadService.active) { toast("Дождись завершения текущей операции"); return }
        hideKeyboard(); urlField?.clearFocus()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean("notificationAsked", false)) {
            prefs.edit().putBoolean("notificationAsked", true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        runCatching { startForegroundService(command) }.onSuccess {
            busy = true; latestMessage = "Подготовка загрузчика…"; latestProgress = -1; resultState = "working"
            updateStatus()
        }.onFailure { toast("Не удалось запустить: ${it.message}") }
    }

    private fun openVideo(video: SavedVideo, share: Boolean) {
        val uri = Uri.parse(video.uri)
        val exists = runCatching { contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)
        if (!exists) { toast("Файл удалён или недоступен в галерее"); return }
        val command = Intent(if (share) Intent.ACTION_SEND else Intent.ACTION_VIEW).apply {
            if (share) { type = "video/*"; putExtra(Intent.EXTRA_STREAM, uri) }
            else setDataAndType(uri, "video/*")
            clipData = ClipData.newRawUri("Видео", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(Intent.createChooser(command, if (share) "Поделиться видео" else "Открыть видео")) }
            .onFailure { toast("Не найдено приложение для видео") }
    }

    private fun section(title: String, tag: String) {
        val heading = row()
        heading.addView(text(title, 16f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(text(tag, 10f, muted).apply { letterSpacing = .06f })
        page.addView(heading, spaced(top = 24, bottom = 12))
    }

    private fun action(label: String, iconName: String, primary: Boolean, click: () -> Unit): LinearLayout = row().apply {
        gravity = Gravity.CENTER
        minimumHeight = dp(54)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = RippleDrawable(ColorStateList.valueOf(0x25ffffff),
            shape(if (primary) mint else surface, 16, if (primary) mint else border), null)
        addView(icon(iconName, if (primary) bg else mint, 20))
        addView(text(label, 14f, if (primary) bg else ink, true),
            LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(9) })
        contentDescription = label
        setOnClickListener { click() }
        isFocusable = true
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: android.view.accessibility.AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.Button"
            }
        }
    }

    private fun iconAction(name: String, label: String, click: () -> Unit): View = icon(name, muted, 48).apply {
        setPadding(dp(14), dp(14), dp(14), dp(14))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = label; isFocusable = true
        background = RippleDrawable(ColorStateList.valueOf(0x25ffffff), shape(surface, 12), null)
        setOnClickListener { click() }
    }

    private fun icon(name: String, color: Int, size: Int) = LineIcon(this, name, color).apply {
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        includeFontPadding = false
    }
    private fun shape(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }
    private fun spaced(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(top); bottomMargin = dp(bottom)
    }
    private fun hideKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(root.windowToken, 0)
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (resources.displayMetrics.density * value + .5f).toInt()
}
