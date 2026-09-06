package com.walcker.games.features.data.shared.source

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import kotlinx.coroutines.flow.Flow

internal interface NotificationSource {
    suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): List<NotificationHistoryItem>

    fun observeHasUnread(userId: String): Flow<Boolean>

    suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    )

    suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    )
}
