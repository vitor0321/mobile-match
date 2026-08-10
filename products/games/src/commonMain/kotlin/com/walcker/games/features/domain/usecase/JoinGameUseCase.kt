package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.repository.GameRepository

internal interface JoinGameUseCase {
    suspend operator fun invoke(gameId: String): Result<Unit>
}

internal class JoinGameUseCaseImpl(
    private val repository: GameRepository,
) : JoinGameUseCase {
    override suspend operator fun invoke(gameId: String): Result<Unit> =
        repository.joinGame(gameId)
}
