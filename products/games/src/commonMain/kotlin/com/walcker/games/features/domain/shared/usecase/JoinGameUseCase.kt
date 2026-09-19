package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface JoinGameUseCase {
    suspend operator fun invoke(gameId: String): Result<JoinMatchOutcome>
}

internal class JoinGameUseCaseImpl(
    private val repository: GameRepository,
) : JoinGameUseCase {
    override suspend operator fun invoke(gameId: String): Result<JoinMatchOutcome> = repository.joinGame(gameId)
}
