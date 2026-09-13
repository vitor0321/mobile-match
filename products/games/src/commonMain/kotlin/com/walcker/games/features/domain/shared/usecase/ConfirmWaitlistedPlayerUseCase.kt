package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface ConfirmWaitlistedPlayerUseCase {
    suspend operator fun invoke(
        matchId: String,
        targetUserId: String,
    ): Result<Unit>
}

internal class ConfirmWaitlistedPlayerUseCaseImpl(
    private val repository: GameRepository,
) : ConfirmWaitlistedPlayerUseCase {
    override suspend operator fun invoke(
        matchId: String,
        targetUserId: String,
    ): Result<Unit> = repository.confirmWaitlistedPlayer(matchId, targetUserId)
}
