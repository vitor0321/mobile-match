package com.walcker.match.app.notifications

internal interface NotificationPermissionRequester {
    suspend fun requestNotificationPermission(): Boolean
}

internal object NotificationPermissionRequesterHolder {
    var requester: NotificationPermissionRequester? = null
}
