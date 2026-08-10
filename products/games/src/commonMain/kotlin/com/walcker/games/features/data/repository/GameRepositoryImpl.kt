package com.walcker.games.features.data.repository

import com.walcker.games.features.data.source.GameSource
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.repository.GameRepository

internal class GameRepositoryImpl(
    private val source: GameSource,
) : GameRepository {

    override suspend fun openGames(): Result<List<Game>> =
        runCatching { source.openGames() }

    override suspend fun joinGame(gameId: String): Result<Unit> =
        runCatching { source.joinGame(gameId) }
}
