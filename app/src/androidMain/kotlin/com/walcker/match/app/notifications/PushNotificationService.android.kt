package com.walcker.match.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.walcker.match.app.MainActivity
import com.walcker.match.app.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class AndroidPushNotificationService(
    private val context: Context,
) : PushNotificationService {
    private val _deviceToken = MutableSharedFlow<String?>(replay = 1)
    override val deviceToken: Flow<String?> = _deviceToken.asSharedFlow()
    override val platform: String = "android"

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

            return@runCatching NotificationPermissionRequesterHolder.requester
                ?.requestNotificationPermission() ?: false
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

        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val matchId = message.data["matchId"]

        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (matchId != null) putExtra(EXTRA_MATCH_ID, matchId)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                matchId?.hashCode() ?: 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(this, MATCH_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher_monochrome)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(matchId?.hashCode() ?: 0, notification)
        }
    }
}

internal const val EXTRA_MATCH_ID = "extra_match_id"
