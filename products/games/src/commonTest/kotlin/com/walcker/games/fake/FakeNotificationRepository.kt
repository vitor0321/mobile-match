package com.walcker.games.fake

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.games.features.domain.shared.repository.NotificationRepository

internal class FakeNotificationRepository(
    var historyResult: Result<List<NotificationHistoryItem>> = Result.success(emptyList()),
    var markAsReadResult: Result<Unit> = Result.success(Unit),
    var deleteResult: Result<Unit> = Result.success(Unit),
) : NotificationRepository {
    val markAsReadCalls: MutableList<String> = mutableListOf()
    val deleteCalls: MutableList<String> = mutableListOf()

    override suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>> = historyResult

    override suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    ): Result<Unit> {
        markAsReadCalls += notificationId
        return markAsReadResult
    }

    override suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    ): Result<Unit> {
        deleteCalls += notificationId
        return deleteResult
    }
}
