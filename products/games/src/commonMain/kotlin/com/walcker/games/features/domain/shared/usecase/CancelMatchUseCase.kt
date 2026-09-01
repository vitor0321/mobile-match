package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface CancelMatchUseCase {
    suspend operator fun invoke(gameId: String): Result<CancelMatchOutcome>
}

internal class CancelMatchUseCaseImpl(
    private val repository: GameRepository,
) : CancelMatchUseCase {
    override suspend operator fun invoke(gameId: String): Result<CancelMatchOutcome> = repository.cancelMatch(gameId)
}
