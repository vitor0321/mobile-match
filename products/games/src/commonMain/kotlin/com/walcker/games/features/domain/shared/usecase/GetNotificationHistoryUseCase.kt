package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.data.shared.model.NotificationHistoryItem

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
    private val repository: com.walcker.games.features.domain.shared.repository.NotificationRepository,
) : GetNotificationHistoryUseCase {
    override suspend fun invoke(
        userId: String,
        limit: Int,
    ): Result<List<NotificationHistoryItem>> = repository.getNotificationHistory(userId, limit)
}
