package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.LeaveMatchOutcome
import com.walcker.games.features.domain.repository.GameRepository

internal interface LeaveMatchUseCase {
    suspend operator fun invoke(gameId: String): Result<LeaveMatchOutcome>
}

internal class LeaveMatchUseCaseImpl(
    private val repository: GameRepository,
) : LeaveMatchUseCase {
    override suspend operator fun invoke(gameId: String): Result<LeaveMatchOutcome> =
        repository.leaveMatch(gameId)
}
