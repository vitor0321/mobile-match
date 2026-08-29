package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.repository.GameRepository

internal interface CancelMatchUseCase {
    suspend operator fun invoke(gameId: String): Result<CancelMatchOutcome>
}

internal class CancelMatchUseCaseImpl(
    private val repository: GameRepository,
) : CancelMatchUseCase {
    override suspend operator fun invoke(gameId: String): Result<CancelMatchOutcome> =
        repository.cancelMatch(gameId)
}
