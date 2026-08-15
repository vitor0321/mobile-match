package com.walcker.games.features.data.source

import com.walcker.match.firestore.FirestoreClient

internal class FirestoreNotificationSource(
    private val firestore: FirestoreClient,
) : NotificationSource {

    override suspend fun updatePushToken(userId: String, token: String) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        firestore.document("users/$userId/pushToken").set(
            data = mapOf(
                "token" to token,
                "platform" to getPlatformName(),
                "updatedAt" to now,
            ),
        ).getOrThrow()
    }
}

internal expect fun getPlatformName(): String
