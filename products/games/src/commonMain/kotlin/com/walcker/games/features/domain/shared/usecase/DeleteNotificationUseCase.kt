package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.repository.NotificationRepository

internal interface DeleteNotificationUseCase {
    suspend operator fun invoke(
        userId: String,
        notificationId: String,
    ): Result<Unit>
}

internal class DeleteNotificationUseCaseImpl(
    private val repository: NotificationRepository,
) : DeleteNotificationUseCase {
    override suspend fun invoke(
        userId: String,
        notificationId: String,
    ): Result<Unit> = repository.deleteNotification(userId, notificationId)
}
