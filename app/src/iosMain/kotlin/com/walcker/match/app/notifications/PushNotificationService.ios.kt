package com.walcker.match.app.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.Result

/**
 * iOS implementation of PushNotificationService.
 *
 * This is a Kotlin wrapper that communicates with Swift code in AppDelegate
 * via a callback mechanism. The actual FCM setup happens in Swift.
 *
 * The Swift AppDelegate will call [onTokenReceived] when a token arrives.
 */
internal class IosPushNotificationService : PushNotificationService {

    private val _deviceToken = MutableSharedFlow<String?>(replay = 1)
    override val deviceToken: Flow<String?> = _deviceToken.asSharedFlow()

    override suspend fun requestNotificationPermission(): Result<Boolean> = runCatching {
        // iOS notification permission request is handled in Swift via UNUserNotificationCenter
        // This method is a no-op at the Kotlin level; iOS code has already requested permission
        // Return true assuming permission was granted (or will be granted by user)
        true
    }

    /**
     * Called from Swift AppDelegate when Firebase Messaging provides a token.
     *
     * Swift code (AppDelegate.swift):
     * ```swift
     * func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
     *     if let fcmToken = fcmToken {
     *         iosPushNotificationService?.onTokenReceived(token: fcmToken)
     *     }
     * }
     * ```
     */
    fun onTokenReceived(token: String?) {
        _deviceToken.tryEmit(token)
    }

    companion object {
        private var instance: IosPushNotificationService? = null

        fun getInstance(): IosPushNotificationService {
            if (instance == null) {
                instance = IosPushNotificationService()
            }
            return instance!!
        }
    }
}
