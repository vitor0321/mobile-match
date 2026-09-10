package com.walcker.games.features.data.shared.source

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class FirestoreNotificationSource(
    private val firestore: FirestoreClient,
) : NotificationSource {
    override fun observeHasUnread(userId: String): Flow<Boolean> {
        val collection = firestore.collection("users/$userId/notificationHistory")
        return collection
            .snapshots(collection.query().where("isRead", "==", false).limit(1))
            .map { result -> result.getOrNull()?.isNotEmpty() == true }
            .catch { emit(false) }
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
                        id = snapshot.id,
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
        firestore
            .document("users/$userId/notificationHistory/$notificationId")
            .update(
                data = mapOf("isRead" to true),
            ).getOrThrow()
    }

    override suspend fun deleteNotification(
        userId: String,
        notificationId: String,
    ) {
        firestore
            .document("users/$userId/notificationHistory/$notificationId")
            .delete()
            .getOrThrow()
    }
}
