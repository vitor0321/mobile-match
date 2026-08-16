package com.walcker.games.features.domain.repository

import com.walcker.games.features.data.model.NotificationHistoryItem

/**
 * Repository for managing push notifications: state, preferences, and history.
 *
 * Phase3-ETAPA1: Token management
 * Phase3-ETAPA2: Notification history
 * Phase3-ETAPA3: Deep linking + mark as read
 */
internal interface NotificationRepository {
    /**
     * Updates the device push token in Firestore: users/{userId}/pushToken
     *
     * @param userId ID of the user
     * @param token Device token from FCM/APNS
     */
    suspend fun updatePushToken(userId: String, token: String): Result<Unit>

    /**
     * Fetches the user's notification history from Firestore.
     *
     * @param userId ID of the user
     * @param limit Maximum number of notifications to return (default 50)
     * @return List of notifications, ordered by most recent first
     */
    suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>>
}
