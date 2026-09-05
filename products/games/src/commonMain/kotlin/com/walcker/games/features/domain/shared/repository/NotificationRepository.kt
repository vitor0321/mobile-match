package com.walcker.games.features.domain.shared.repository

import com.walcker.games.features.data.shared.model.NotificationHistoryItem

internal interface NotificationRepository {
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
