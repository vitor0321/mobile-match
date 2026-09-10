package com.walcker.match.app.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class IosPushNotificationService : PushNotificationService {
    private val _deviceToken = MutableSharedFlow<String?>(replay = 1)
    override val deviceToken: Flow<String?> = _deviceToken.asSharedFlow()
    override val platform: String = "ios"

    override suspend fun requestNotificationPermission(): Result<Boolean> =
        runCatching {
            true
        }

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
