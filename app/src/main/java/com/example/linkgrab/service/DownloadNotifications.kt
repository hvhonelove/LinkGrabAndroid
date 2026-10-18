package com.example.linkgrab.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.linkgrab.MainActivity
import com.example.linkgrab.R

class DownloadNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init { manager.createNotificationChannel(NotificationChannel(CHANNEL, "Загрузки LinkGrab", NotificationManager.IMPORTANCE_LOW)) }
    fun build(title: String, percent: Int = -1, id: Long = 0): Notification {
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(context, 1, Intent(context, DownloadService::class.java)
            .setAction(DownloadService.PAUSE).putExtra("id", id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("LinkGrab").setContentText(title).setContentIntent(open)
            .setProgress(100, percent.coerceAtLeast(0), percent < 0).setOnlyAlertOnce(true).setOngoing(true)
            .apply { if (id > 0) addAction(0, "Пауза", stop) }.build()
    }
    fun progress(title: String, percent: Int, id: Long) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        manager.notify(FOREGROUND_ID, build(title, percent, id))
    }
    fun finished(title: String, uri: String, mime: String, id: Long) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val open = PendingIntent.getActivity(context, id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify((id + 1000).toInt(), NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Готово · LinkGrab").setContentText(title).setAutoCancel(true).setContentIntent(open).build())
    }
    companion object { const val FOREGROUND_ID = 101; private const val CHANNEL = "linkgrab_v2_downloads" }
}
