package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game

internal interface GameSource {
    suspend fun openGames(): List<Game>
    suspend fun joinGame(gameId: String)
    suspend fun createMatch(request: CreateMatchRequest): String

    /**
     * Matches where the user is in the [Game.participants] list.
     * Server is expected to merge organizer + participants.
     */
    suspend fun matchesForUser(userId: String): List<Game>
}
