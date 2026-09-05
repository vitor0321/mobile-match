package com.walcker.games.features.ui.shared.notifications

import com.walcker.games.features.data.shared.model.NotificationHistoryItem

internal data class NotificationHistoryState(
    val notifications: List<NotificationHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

internal sealed interface NotificationHistoryEvent {
    data object Refresh : NotificationHistoryEvent

    data object DismissError : NotificationHistoryEvent

    data class MarkAsRead(
        val id: String,
    ) : NotificationHistoryEvent

    data class Delete(
        val id: String,
    ) : NotificationHistoryEvent
}
