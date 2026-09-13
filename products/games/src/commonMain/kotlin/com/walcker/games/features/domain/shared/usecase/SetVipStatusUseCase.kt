package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface SetVipStatusUseCase {
    suspend operator fun invoke(
        matchId: String,
        targetUserId: String,
        isVip: Boolean,
    ): Result<Unit>
}

internal class SetVipStatusUseCaseImpl(
    private val repository: GameRepository,
) : SetVipStatusUseCase {
    override suspend operator fun invoke(
        matchId: String,
        targetUserId: String,
        isVip: Boolean,
    ): Result<Unit> = repository.setVipStatus(matchId, targetUserId, isVip)
}
