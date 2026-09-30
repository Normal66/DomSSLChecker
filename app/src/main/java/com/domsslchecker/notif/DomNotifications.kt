package com.domsslchecker.notif

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object DomNotifications {
    const val CHANNEL_ID: String = "domsslchecker_expiry"

    fun init(app: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Контроль сроков",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        ch.description = "Напоминания об истечении регистрации домена и SSL"
        mgr.createNotificationChannel(ch)
    }
}
