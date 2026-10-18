package com.example.linkgrab

import android.app.Application
import androidx.room.Room
import com.example.linkgrab.data.db.LinkGrabDatabase
import com.example.linkgrab.data.engine.YtDlpEngine
import com.example.linkgrab.data.engine.EngineUpdateCoordinator
import com.example.linkgrab.data.repository.DownloadRepository
import com.example.linkgrab.data.settings.SettingsStore
import com.example.linkgrab.data.storage.MediaStoreStorage
import kotlinx.coroutines.*

class AppContainer(application: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database = Room.databaseBuilder(application, LinkGrabDatabase::class.java, "linkgrab.db").build()
    val settings = SettingsStore(application)
    val engine = YtDlpEngine(application)
    val engineUpdates = EngineUpdateCoordinator(application, engine, settings)
    val storage = MediaStoreStorage(application)
    val downloads = DownloadRepository(application, database, storage, engine, settings)
    val ready = scope.async { downloads.recover() }
}
class LinkGrabApplication : Application() {
    val container by lazy { AppContainer(this) }
    override fun onCreate() { super.onCreate(); container }
}
