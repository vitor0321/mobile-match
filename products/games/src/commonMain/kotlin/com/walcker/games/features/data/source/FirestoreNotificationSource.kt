package com.walcker.games.features.data.source

import com.walcker.games.features.data.model.NotificationHistoryItem
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

    override suspend fun getNotificationHistory(
        userId: String,
        limit: Int,
    ): List<NotificationHistoryItem> {
        return firestore
            .collection("users/$userId/notificationHistory")
            .query()
            .orderBy("receivedAt", direction = "desc")
            .limit(limit)
            .get()
            .getOrNull()
            ?.mapNotNull { snapshot ->
                try {
                    NotificationHistoryItem(
                        id = snapshot.getString("id") ?: return@mapNotNull null,
                        title = snapshot.getString("title") ?: "",
                        body = snapshot.getString("body") ?: "",
                        receivedAt = snapshot.getLong("receivedAt") ?: 0L,
                        isRead = snapshot.getBoolean("isRead") ?: false,
                        data = (snapshot.get("data") as? Map<String, String>) ?: emptyMap(),
                    )
                } catch (e: Exception) {
                    null
                }
            }
            ?: emptyList()
    }

    override suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String,
    ) {
        firestore.document("users/$userId/notificationHistory/$notificationId").update(
            data = mapOf("isRead" to true),
        ).getOrThrow()
    }

    override suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    ) {
        firestore.document("users/$userId/notificationHistory/$notificationId").delete()
            .getOrThrow()
    }
}

internal expect fun getPlatformName(): String
