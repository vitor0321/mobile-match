package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.repository.GameRepository

/**
 * Cancels a match the user organizes. Server-side enforcement via the
 * `cancelMatch` Cloud Function — only the organizer can cancel.
 */
internal interface CancelMatchUseCase {
    suspend operator fun invoke(gameId: String): Result<CancelMatchOutcome>
}

internal class CancelMatchUseCaseImpl(
    private val repository: GameRepository,
) : CancelMatchUseCase {
    override suspend operator fun invoke(gameId: String): Result<CancelMatchOutcome> =
        repository.cancelMatch(gameId)
}
