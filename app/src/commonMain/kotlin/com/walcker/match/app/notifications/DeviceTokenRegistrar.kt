package com.walcker.match.app.notifications

import com.walcker.identity.api.SessionHolder
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.flow.collectLatest

internal class DeviceTokenRegistrar(
    private val sessionHolder: SessionHolder,
    private val pushNotificationService: PushNotificationService,
    private val firestore: FirestoreClient,
) {
    suspend fun start() {
        sessionHolder.currentUser.collectLatest { session ->
            if (session == null) return@collectLatest

            pushNotificationService.requestNotificationPermission()

            pushNotificationService.deviceToken.collectLatest { token ->
                if (token.isNullOrBlank()) return@collectLatest
                firestore.document("users/${session.uid}/devices/$token").set(
                    mapOf(
                        "userId" to session.uid,
                        "platform" to pushNotificationService.platform,
                    ),
                    merge = true,
                )
            }
        }
    }
}
