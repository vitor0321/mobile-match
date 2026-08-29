package com.walcker.games.features.data.source

import com.walcker.games.features.data.model.NotificationHistoryItem

internal interface NotificationSource {
    suspend fun updatePushToken(userId: String, token: String)

    suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): List<NotificationHistoryItem>

    suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    )

    suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    )
}
