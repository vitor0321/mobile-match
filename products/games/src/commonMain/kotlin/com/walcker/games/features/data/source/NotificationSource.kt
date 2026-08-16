package com.walcker.games.features.data.source

import com.walcker.games.features.data.model.NotificationHistoryItem

/**
 * Data source for notifications.
 *
 * Responsible for:
 * - Persisting push tokens to Firestore
 * - Fetching notification history from Firestore
 * - Managing notification preferences
 */
internal interface NotificationSource {
    /**
     * Updates the device push token in Firestore: users/{userId}/pushToken
     *
     * @param userId ID of the user
     * @param token Device token from FCM/APNS
     */
    suspend fun updatePushToken(userId: String, token: String)

    /**
     * Fetches the user's notification history from Firestore.
     *
     * @param userId ID of the user
     * @param limit Maximum number of notifications to return
     * @return List of notifications, ordered by most recent first
     */
    suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): List<NotificationHistoryItem>
}
