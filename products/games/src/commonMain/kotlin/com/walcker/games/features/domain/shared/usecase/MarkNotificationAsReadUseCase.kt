package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.repository.NotificationRepository

internal interface MarkNotificationAsReadUseCase {
    suspend operator fun invoke(
        userId: String,
        notificationId: String,
    ): Result<Unit>
}

internal class MarkNotificationAsReadUseCaseImpl(
    private val repository: NotificationRepository,
) : MarkNotificationAsReadUseCase {
    override suspend fun invoke(
        userId: String,
        notificationId: String,
    ): Result<Unit> = repository.markNotificationAsRead(userId, notificationId)
}
