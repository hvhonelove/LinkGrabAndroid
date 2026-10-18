package com.example.linkgrab.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.example.linkgrab.LinkGrabApplication
import kotlinx.coroutines.*

class DownloadService : Service() {
    private val graph get() = (application as LinkGrabApplication).container
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var runner: Job? = null
    private var generation = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notifications: DownloadNotifications
    override fun onCreate() { super.onCreate(); notifications = DownloadNotifications(this) }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == PAUSE) {
            graph.scope.launch { graph.downloads.pause(intent.getLongExtra("id", 0)) }
            if (runner?.isActive != true) stopSelf(startId)
            return START_NOT_STICKY
        }
        startForeground(DownloadNotifications.FOREGROUND_ID, notifications.build("Подготовка очереди…"))
        generation++
        if (runner?.isActive == true) return START_NOT_STICKY
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LinkGrab:Queue").apply {
            acquire(6 * 60 * 60 * 1000L)
        }
        runner = scope.launch {
            try {
                graph.ready.await()
                val processor = QueueProcessor(graph, notifications)
                while (isActive) {
                    val epoch = generation
                    val next = graph.downloads.dao.next()
                    if (next == null) { if (epoch != generation) continue else break }
                    processor.runOne(next)
                }
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        runner?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
    companion object {
        const val PAUSE = "com.example.linkgrab.PAUSE"
        fun start(context: Context) { context.startForegroundService(Intent(context, DownloadService::class.java)) }
    }
}
