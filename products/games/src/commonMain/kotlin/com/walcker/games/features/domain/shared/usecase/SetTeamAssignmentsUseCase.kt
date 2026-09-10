package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface SetTeamAssignmentsUseCase {
    suspend operator fun invoke(
        matchId: String,
        teamCount: Int,
        assignments: Map<String, Int>,
    ): Result<Unit>
}

internal class SetTeamAssignmentsUseCaseImpl(
    private val repository: GameRepository,
) : SetTeamAssignmentsUseCase {
    override suspend operator fun invoke(
        matchId: String,
        teamCount: Int,
        assignments: Map<String, Int>,
    ): Result<Unit> = repository.setTeamAssignments(matchId, teamCount, assignments)
}
