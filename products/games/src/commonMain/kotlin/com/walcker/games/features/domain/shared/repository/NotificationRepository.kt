package com.walcker.games.features.domain.shared.repository

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import kotlinx.coroutines.flow.Flow

internal interface NotificationRepository {
    suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>>

    fun observeHasUnread(userId: String): Flow<Boolean>

    suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    ): Result<Unit>

    suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    ): Result<Unit>
}
