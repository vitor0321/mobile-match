package com.walcker.games.features.data.source

/**
 * Data source for notifications.
 *
 * Responsible for persisting push tokens and notification preferences
 * to Firestore and local storage.
 */
internal interface NotificationSource {
    /**
     * Updates the device push token in Firestore: users/{userId}/pushToken
     *
     * @param userId ID of the user
     * @param token Device token from FCM/APNS
     */
    suspend fun updatePushToken(userId: String, token: String)
}
