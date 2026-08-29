package com.walcker.games.features.domain.usecase

internal interface UpdatePushTokenUseCase {
    suspend operator fun invoke(userId: String, token: String): Result<Unit>
}

internal class UpdatePushTokenUseCaseImpl(
    private val notificationRepository: com.walcker.games.features.domain.repository.NotificationRepository,
) : UpdatePushTokenUseCase {
    override suspend operator fun invoke(userId: String, token: String): Result<Unit> {
        return notificationRepository.updatePushToken(userId, token)
    }
}
