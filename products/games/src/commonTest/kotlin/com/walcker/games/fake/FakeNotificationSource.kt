package com.walcker.games.fake

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.games.features.data.shared.source.NotificationSource

internal class FakeNotificationSource(
    var historyResult: () -> List<NotificationHistoryItem> = { emptyList() },
    var markAsReadResult: () -> Unit = {},
    var deleteResult: () -> Unit = {},
) : NotificationSource {
    var markAsReadCallCount: Int = 0
        private set
    var deleteCallCount: Int = 0
        private set

    override suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): List<NotificationHistoryItem> = historyResult()

    override suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    ) {
        markAsReadCallCount++
        markAsReadResult()
    }

    override suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    ) {
        deleteCallCount++
        deleteResult()
    }
}
