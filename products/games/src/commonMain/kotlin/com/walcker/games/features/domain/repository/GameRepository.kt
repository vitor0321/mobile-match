package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.Game

internal interface GameRepository {
    suspend fun openGames(): Result<List<Game>>
    suspend fun joinGame(gameId: String): Result<Unit>
}
