package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.repository.NotificationRepository

internal interface MarkNotificationAsReadUseCase {
    suspend operator fun invoke(userId: String, notificationId: String): Result<Unit>
}

internal class MarkNotificationAsReadUseCaseImpl(
    private val repository: NotificationRepository,
) : MarkNotificationAsReadUseCase {
    override suspend fun invoke(userId: String, notificationId: String): Result<Unit> {
        return repository.markNotificationAsRead(userId, notificationId)
    }
}
