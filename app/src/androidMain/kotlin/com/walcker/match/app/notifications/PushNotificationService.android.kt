package com.walcker.match.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class AndroidPushNotificationService(
    private val context: Context,
) : PushNotificationService {
    private val _deviceToken = MutableSharedFlow<String?>(replay = 1)
    override val deviceToken: Flow<String?> = _deviceToken.asSharedFlow()

    init {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                _deviceToken.tryEmit(token)
            }
        }

        tokenUpdateCallback = { token ->
            _deviceToken.tryEmit(token)
        }
    }

    override suspend fun requestNotificationPermission(): Result<Boolean> =
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@runCatching true
            }

            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                return@runCatching true
            }

            return@runCatching false
        }
}

internal var tokenUpdateCallback: ((String) -> Unit)? = null

internal class FirebasePushNotificationService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        tokenUpdateCallback?.invoke(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        android.util.Log.d("FCM", "Message received: ${message.data}")
    }
}
