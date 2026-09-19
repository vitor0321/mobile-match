package com.walcker.games.features.domain.create.usecase

import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface UpdateMatchUseCase {
    suspend operator fun invoke(
        matchId: String,
        request: CreateMatchRequest,
    ): Result<Unit>
}

internal class UpdateMatchUseCaseImpl(
    private val repository: GameRepository,
) : UpdateMatchUseCase {
    override suspend fun invoke(
        matchId: String,
        request: CreateMatchRequest,
    ): Result<Unit> = repository.updateMatch(matchId, request)
}
