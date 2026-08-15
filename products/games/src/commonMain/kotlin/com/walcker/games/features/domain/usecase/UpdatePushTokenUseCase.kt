package com.walcker.games.features.domain.usecase

/**
 * Updates the user's device push token in the repository.
 *
 * Called when:
 * - App starts (request current token from PushNotificationService)
 * - Token is refreshed by Firebase
 *
 * Stores token in Firestore: users/{userId}/pushToken
 */
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
