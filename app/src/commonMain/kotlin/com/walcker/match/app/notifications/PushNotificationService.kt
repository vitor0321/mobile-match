package com.walcker.match.app.notifications

import kotlinx.coroutines.flow.Flow

internal interface PushNotificationService {
    val deviceToken: Flow<String?>

    suspend fun requestNotificationPermission(): Result<Boolean>
}
