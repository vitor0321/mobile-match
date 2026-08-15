package com.walcker.match.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android implementation of PushNotificationService using Firebase Cloud Messaging.
 *
 * Singleton that wraps FirebaseMessaging callbacks and propagates tokens to Flow.
 */
internal class AndroidPushNotificationService(
    private val context: Context,
) : PushNotificationService {

    private val _deviceToken = MutableSharedFlow<String?>(replay = 1)
    override val deviceToken: Flow<String?> = _deviceToken.asSharedFlow()

    init {
        // Fetch current token on initialization
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                _deviceToken.tryEmit(token)
            }
        }

        // Register as global token update listener (called from FirebasePushNotificationService.onNewToken)
        tokenUpdateCallback = { token ->
            _deviceToken.tryEmit(token)
        }
    }

    override suspend fun requestNotificationPermission(): Result<Boolean> = runCatching {
        // On Android 12 and below, notifications are enabled by default
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@runCatching true
        }

        // On Android 13+, request POST_NOTIFICATIONS permission
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return@runCatching true
        }

        // For runtime request, we'd need Activity context with requestPermissions().
        // For now, assume user grants it during install. Proper flow requires
        // launcher.requestPermissionLauncher in MainActivity.
        // Return false to indicate we couldn't request (Activity-level permission needed)
        return@runCatching false
    }
}

/**
 * Callback registered by AndroidPushNotificationService to receive token updates
 */
internal var tokenUpdateCallback: ((String) -> Unit)? = null

/**
 * Firebase Cloud Messaging Service for handling incoming push messages.
 *
 * Called when:
 * 1. App receives a message from Firebase Cloud Messaging
 * 2. onNewToken is called when token is refreshed
 *
 * This is a stub implementation that can be extended to show notifications
 * or handle data messages in ETAPA3.
 */
internal class FirebasePushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Notify the AndroidPushNotificationService singleton of the new token
        tokenUpdateCallback?.invoke(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // In ETAPA3, we'll implement local notification display here
        // For now, just log that we received a message
        android.util.Log.d("FCM", "Message received: ${message.data}")
    }
}
