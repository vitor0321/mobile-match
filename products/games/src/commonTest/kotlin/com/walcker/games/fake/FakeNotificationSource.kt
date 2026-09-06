package com.walcker.games.fake

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.games.features.data.shared.source.NotificationSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeNotificationSource(
    var historyResult: () -> List<NotificationHistoryItem> = { emptyList() },
    var markAsReadResult: () -> Unit = {},
    var deleteResult: () -> Unit = {},
    var hasUnreadFlow: Flow<Boolean> = flowOf(false),
) : NotificationSource {
    var markAsReadCallCount: Int = 0
        private set
    var deleteCallCount: Int = 0
        private set

    override fun observeHasUnread(userId: String): Flow<Boolean> = hasUnreadFlow

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
