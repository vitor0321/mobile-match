package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Game

internal interface GameSource {
    suspend fun openGames(): List<Game>
    suspend fun joinGame(gameId: String)
}
