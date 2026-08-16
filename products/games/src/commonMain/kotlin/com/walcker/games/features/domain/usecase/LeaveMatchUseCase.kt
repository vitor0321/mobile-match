package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.LeaveMatchOutcome
import com.walcker.games.features.domain.repository.GameRepository

/**
 * Leaves a match the user joined. Server-side enforcement via the
 * `leaveMatch` Cloud Function — automatically promotes the first FIFO from
 * waitlist if the user was confirmed (rule B3 of the migration plan).
 */
internal interface LeaveMatchUseCase {
    suspend operator fun invoke(gameId: String): Result<LeaveMatchOutcome>
}

internal class LeaveMatchUseCaseImpl(
    private val repository: GameRepository,
) : LeaveMatchUseCase {
    override suspend operator fun invoke(gameId: String): Result<LeaveMatchOutcome> =
        repository.leaveMatch(gameId)
}
