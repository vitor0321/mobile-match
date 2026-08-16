package com.walcker.games.features.data.repository

import com.walcker.games.features.data.model.NotificationHistoryItem
import com.walcker.games.features.data.source.NotificationSource
import com.walcker.games.features.domain.repository.NotificationRepository

internal class NotificationRepositoryImpl(
    private val source: NotificationSource,
) : NotificationRepository {

    override suspend fun updatePushToken(userId: String, token: String): Result<Unit> {
        return runCatching {
            source.updatePushToken(userId, token)
        }
    }

    override suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>> {
        return runCatching {
            source.getNotificationHistory(userId, limit)
        }
    }
}
