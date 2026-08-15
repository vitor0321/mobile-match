package com.walcker.games.features.domain.repository

/**
 * Repository for managing push notification state and preferences.
 *
 * Phase3-ETAPA1: Token management only
 * Phase3-ETAPA2: Add notification preferences
 */
internal interface NotificationRepository {
    /**
     * Updates the device push token in Firestore: users/{userId}/pushToken
     *
     * @param userId ID of the user
     * @param token Device token from FCM/APNS
     */
    suspend fun updatePushToken(userId: String, token: String): Result<Unit>
}
