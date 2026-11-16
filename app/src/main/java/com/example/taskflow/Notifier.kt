package com.example.taskflow

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

object Notifier {
    fun show(ctx: Context, title: String, text: String) {
        // Android 13+ : on vérifie que l'utilisateur a autorisé les notifications
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(NotificationChannel("tasks", "Tâches", NotificationManager.IMPORTANCE_DEFAULT))
        val n = NotificationCompat.Builder(ctx, "tasks")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title).setContentText(text).setAutoCancel(true).build()
        nm.notify(System.currentTimeMillis().toInt(), n)
    }
}