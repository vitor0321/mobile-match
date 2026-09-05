package com.walcker.games.features.data.shared.repository

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.games.features.data.shared.source.NotificationSource
import com.walcker.games.features.domain.shared.repository.NotificationRepository

internal class NotificationRepositoryImpl(
    private val source: NotificationSource,
) : NotificationRepository {
    override suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>> =
        runCatching {
            source.getNotificationHistory(userId, limit)
        }

    override suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    ): Result<Unit> =
        runCatching {
            source.markNotificationAsRead(userId, notificationId)
        }

    override suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    ): Result<Unit> =
        runCatching {
            source.deleteNotification(userId, notificationId)
        }
}
