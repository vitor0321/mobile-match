package com.walcker.match.app.notifications

import com.walcker.identity.api.SessionHolder
import com.walcker.identity.api.SignOutCleanup
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

internal class DeviceTokenRegistrar(
    private val sessionHolder: SessionHolder,
    private val pushNotificationService: PushNotificationService,
    private val firestore: FirestoreClient,
) : SignOutCleanup {
    private val registeredDevice = MutableStateFlow<RegisteredDevice?>(null)

    suspend fun start() {
        sessionHolder.currentUser.collectLatest { session ->
            if (session == null) {
                registeredDevice.value = null
                return@collectLatest
            }

            pushNotificationService.requestNotificationPermission()

            pushNotificationService.deviceToken.collectLatest { token ->
                if (token.isNullOrBlank()) return@collectLatest
                val device = RegisteredDevice(userId = session.uid, token = token)
                registeredDevice.value
                    ?.takeIf { it.userId == device.userId && it.token != device.token }
                    ?.let { staleDevice -> firestore.document(staleDevice.path).delete() }
                firestore
                    .document(device.path)
                    .set(
                        mapOf(
                            "userId" to device.userId,
                            "platform" to pushNotificationService.platform,
                        ),
                        merge = true,
                    ).onSuccess { registeredDevice.value = device }
            }
        }
    }

    override suspend fun beforeSignOut() {
        val device = registeredDevice.value ?: return
        firestore.document(device.path).delete()
        registeredDevice.value = null
    }

    private data class RegisteredDevice(
        val userId: String,
        val token: String,
    ) {
        val path: String get() = "users/$userId/devices/$token"
    }
}
