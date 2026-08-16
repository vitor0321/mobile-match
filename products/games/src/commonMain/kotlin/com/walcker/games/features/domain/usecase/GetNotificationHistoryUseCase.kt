package com.walcker.games.features.domain.usecase

import com.walcker.games.features.data.model.NotificationHistoryItem

/**
 * Fetch the current user's notification history from Firestore.
 *
 * Ordered by most recent first; limited to [limit] notifications (default 50).
 * Empty list if no notifications found or user not authenticated.
 */
internal interface GetNotificationHistoryUseCase {
    suspend operator fun invoke(
        userId: String,
        limit: Int = DEFAULT_LIMIT,
    ): Result<List<NotificationHistoryItem>>

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}

internal class GetNotificationHistoryUseCaseImpl(
    private val repository: com.walcker.games.features.domain.repository.NotificationRepository,
) : GetNotificationHistoryUseCase {
    override suspend fun invoke(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>> =
        repository.getNotificationHistory(userId, limit)
}
