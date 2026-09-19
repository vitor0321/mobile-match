package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface GetGameByIdUseCase {
    suspend operator fun invoke(gameId: String): Result<Game>
}

internal class GetGameByIdUseCaseImpl(
    private val repository: GameRepository,
) : GetGameByIdUseCase {
    override suspend fun invoke(gameId: String): Result<Game> = repository.getGameById(gameId)
}
