package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface BanPlayerFromMatchUseCase {
    suspend operator fun invoke(
        matchId: String,
        targetUserId: String,
    ): Result<String?>
}

internal class BanPlayerFromMatchUseCaseImpl(
    private val repository: GameRepository,
) : BanPlayerFromMatchUseCase {
    override suspend operator fun invoke(
        matchId: String,
        targetUserId: String,
    ): Result<String?> = repository.banPlayerFromMatch(matchId, targetUserId)
}
