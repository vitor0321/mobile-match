package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.repository.GameRepository

internal interface GetGameByIdUseCase {
    suspend operator fun invoke(gameId: String): Result<Game>
}

internal class GetGameByIdUseCaseImpl(
    private val repository: GameRepository,
) : GetGameByIdUseCase {
    override suspend fun invoke(gameId: String): Result<Game> {
        return repository.getGameById(gameId)
    }
}
