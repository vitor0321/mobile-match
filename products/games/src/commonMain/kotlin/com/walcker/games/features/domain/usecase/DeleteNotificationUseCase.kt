package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.repository.NotificationRepository

internal interface DeleteNotificationUseCase {
    suspend operator fun invoke(userId: String, notificationId: String): Result<Unit>
}

internal class DeleteNotificationUseCaseImpl(
    private val repository: NotificationRepository,
) : DeleteNotificationUseCase {
    override suspend fun invoke(userId: String, notificationId: String): Result<Unit> {
        return repository.deleteNotification(userId, notificationId)
    }
}
