package com.walcker.games.features.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a single notification in the user's notification history.
 *
 * @param id Unique identifier for the notification (typically timestamp-based)
 * @param title Notification title
 * @param body Notification body/content
 * @param receivedAt Timestamp when notification was received (milliseconds since epoch)
 * @param isRead Whether the user has read this notification
 * @param data Additional data payload from the notification
 */
@Serializable
data class NotificationHistoryItem(
    val id: String,
    val title: String,
    val body: String,
    val receivedAt: Long,
    val isRead: Boolean = false,
    val data: Map<String, String> = emptyMap(),
)
