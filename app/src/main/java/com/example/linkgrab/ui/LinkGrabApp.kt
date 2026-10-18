package com.example.linkgrab.ui

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.linkgrab.AppContainer
import com.example.linkgrab.core.LinkParser
import com.example.linkgrab.data.db.DownloadEntity
import com.example.linkgrab.domain.ThemeMode
import com.example.linkgrab.feature.home.*
import com.example.linkgrab.feature.preview.PreviewScreen
import com.example.linkgrab.feature.downloads.*
import com.example.linkgrab.feature.settings.*
import com.example.linkgrab.ui.theme.LinkGrabTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.merge

@Composable
fun LinkGrabApp(graph: AppContainer, home: HomeViewModel, fileAction: (DownloadEntity, FileAction) -> Unit,
    notifications: () -> Unit, openUrl: (String) -> Unit) {
    val downloads: DownloadsViewModel = viewModel(factory = viewModelFactory { initializer { DownloadsViewModel(graph) } })
    val settingsVm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(graph) } })
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val entries by downloads.items.collectAsStateWithLifecycle()
    val analysis by home.analysis.collectAsStateWithLifecycle()
    val submitting by home.submitting.collectAsStateWithLifecycle()
    val link by home.link.collectAsStateWithLifecycle()
    val version by settingsVm.version.collectAsStateWithLifecycle()
    val updating by settingsVm.updating.collectAsStateWithLifecycle()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(analysis.metadata) { if (analysis.metadata != null) { keyboard?.hide(); focus.clearFocus() } }
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var about by rememberSaveable { mutableStateOf(false) }
    var licenses by rememberSaveable { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf<String?>(null) }
    var dismissedClipboard by rememberSaveable { mutableStateOf("") }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { merge(home.events, downloads.events, settingsVm.events).collect { snack.showSnackbar(it) } }
    LaunchedEffect(Unit) { home.sharedNavigation.collect { tab = 0; detailId = null; about = false; licenses = false } }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner, settings?.clipboard) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && settings?.clipboard == true) scope.launch {
                delay(350)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val candidate = runCatching { clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty() }.getOrDefault("")
                val url = LinkParser.extract(candidate)
                if (LinkParser.isValid(url) && url != link && url != dismissedClipboard) suggestion = url
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val detail = entries.find { it.id == detailId }
    val preview = analysis.metadata.takeIf { tab == 0 && detailId == null && !about && !licenses }
    BackHandler(enabled = detailId != null || about || licenses || preview != null) {
        when { licenses -> licenses = false; about -> about = false; detailId != null -> detailId = null; else -> home.closePreview() }
    }
    LinkGrabTheme(settings?.theme ?: ThemeMode.SYSTEM) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val preferences = settings
            when {
                preferences == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                !preferences.onboarded -> OnboardingScreen { settingsVm.flag("onboarded", true) }
                else -> Scaffold(containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snack) }, bottomBar = {
                        if (preview == null && detailId == null && !about && !licenses) BottomTabs(tab) { tab = it }
                    }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        val route = when { licenses -> "licenses"; about -> "about"; detail != null -> "detail"; preview != null -> "preview"; else -> "tab$tab" }
                        AnimatedContent(route, label = "screen") { current ->
                            when (current) {
                                "licenses" -> LicenseScreen(context) { licenses = false }
                                "about" -> AboutScreen({ about = false }, { licenses = true }, { openUrl("https://github.com/hvhonelove/LinkGrabAndroid") })
                                "detail" -> if (detail != null) FileDetailsScreen(detail, { detailId = null },
                                    { fileAction(detail, it) }, { downloads.rename(detail.id, it) },
                                    { downloads.remove(detail.id, it); detailId = null }, { downloads.retry(context, detail.id) },
                                    { downloads.pause(detail.id) }, { downloads.cancel(detail.id) })
                                "preview" -> if (preview != null) PreviewScreen(preview, preferences, submitting,
                                    home::closePreview) { options ->
                                    if (preferences.notifications && !preferences.notificationAsked) {
                                        settingsVm.flag("notificationAsked", true)
                                        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications()
                                    }
                                    home.enqueue(context, options) { tab = 1 }
                                }
                                "tab1" -> DownloadsScreen(entries, { detailId = it }, { downloads.retry(context, it) }, downloads::pause,
                                    downloads::cancel, downloads::next, { downloads.startQueue(context) }, { tab = 0 })
                                "tab2" -> SettingsScreen(preferences, version, updating, settingsVm::text, settingsVm::flag, settingsVm::quality,
                                    settingsVm::updateEngine, settingsVm::refreshVersion, notifications, { about = true })
                                else -> HomeScreen(link, analysis, entries, suggestion, home::setLink,
                                    paste = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val value = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                        if (value.isNotBlank()) home.setLink(LinkParser.extract(value))
                                        else scope.launch { snack.showSnackbar("Буфер обмена пуст") }
                                    }, analyze = home::analyze, showDownloads = { tab = 1 }, details = { detailId = it },
                                    acceptClipboard = { home.setLink(suggestion.orEmpty()); suggestion = null },
                                    dismissClipboard = { dismissedClipboard = suggestion.orEmpty(); suggestion = null })
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun BottomTabs(selected: Int, select: (Int) -> Unit) {
    Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val icons = listOf(Icons.Outlined.Home, Icons.Outlined.Downloading, Icons.Outlined.Tune)
            val labels = listOf("Главная", "Загрузки", "Настройки")
            repeat(3) { index ->
                Surface(Modifier.weight(1f).testTag("nav-$index").clickable { select(index) },
                    shape = RoundedCornerShape(20.dp), color = if (selected == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(icons[index], labels[index], tint = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(labels[index], style = MaterialTheme.typography.bodySmall,
                            color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
@Composable
private fun LicenseScreen(context: Context, back: () -> Unit) {
    val notice by produceState("Загрузка текста лицензий…") {
        value = withContext(Dispatchers.IO) { context.assets.open("licenses.txt").bufferedReader().use { it.readText() } }
    }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, "Назад") }
            Text("Лицензии", style = MaterialTheme.typography.titleMedium)
        }
        androidx.compose.foundation.text.selection.SelectionContainer {
            androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(22.dp)) {
                item { Text(notice, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
