package com.walcker.match.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat

internal const val MATCH_NOTIFICATION_CHANNEL_ID = "matches"

internal fun createMatchNotificationChannel(context: Context) {
    val channel =
        NotificationChannel(
            MATCH_NOTIFICATION_CHANNEL_ID,
            "Partidas",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos de partidas novas perto de você e lembretes das suas partidas"
        }
    val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
    manager?.createNotificationChannel(channel)
}
