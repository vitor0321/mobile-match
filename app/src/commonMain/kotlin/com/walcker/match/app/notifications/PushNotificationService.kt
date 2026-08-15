package com.walcker.match.app.notifications

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for push notification service.
 *
 * Platform implementations handle token registration with FCM/APNS
 * and propagate tokens via [deviceToken] Flow.
 *
 * Implementation:
 * - Android: wraps FirebaseMessagingService
 * - iOS: wraps MessagingDelegate callbacks
 */
internal interface PushNotificationService {
    /**
     * Emits device push tokens as they arrive from Firebase Cloud Messaging.
     * Token is emitted on:
     * - App first start (new device/app)
     * - Token refresh (Firebase rotates token periodically)
     */
    val deviceToken: Flow<String?>

    /**
     * Request user permission for notifications (Android 13+, iOS always needed).
     * @return true if permission granted, false if denied
     */
    suspend fun requestNotificationPermission(): Result<Boolean>
}
