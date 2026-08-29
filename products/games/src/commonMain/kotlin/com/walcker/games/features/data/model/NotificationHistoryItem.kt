package com.walcker.games.features.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationHistoryItem(
    val id: String,
    val title: String,
    val body: String,
    val receivedAt: Long,
    val isRead: Boolean = false,
    val data: Map<String, String> = emptyMap(),
)
