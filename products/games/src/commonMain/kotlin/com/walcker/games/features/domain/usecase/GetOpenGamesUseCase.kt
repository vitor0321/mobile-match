package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.repository.GameRepository

internal interface GetOpenGamesUseCase {
    suspend operator fun invoke(): Result<List<Game>>
}

internal class GetOpenGamesUseCaseImpl(
    private val repository: GameRepository,
) : GetOpenGamesUseCase {
    override suspend operator fun invoke(): Result<List<Game>> =
        repository.openGames().map { games -> games.filter { it.hasOpenSlots } }
}
