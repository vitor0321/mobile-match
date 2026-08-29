package com.walcker.games.features.domain.repository

import com.walcker.games.features.data.model.NotificationHistoryItem

internal interface NotificationRepository {
    suspend fun updatePushToken(userId: String, token: String): Result<Unit>

    suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>>

    suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    ): Result<Unit>

    suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    ): Result<Unit>
}
