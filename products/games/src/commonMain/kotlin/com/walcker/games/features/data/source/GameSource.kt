package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.ParticipantsSummary
import kotlinx.coroutines.flow.Flow

internal interface GameSource {
    suspend fun openGames(): List<Game>
    suspend fun joinGame(gameId: String)
    suspend fun createMatch(request: CreateMatchRequest): String

    /**
     * Matches where the user is in the [Game.participants] list.
     * Server is expected to merge organizer + participants.
     */
    suspend fun matchesForUser(userId: String): List<Game>

    /**
     * Fetches a single game by ID from Firestore.
     */
    suspend fun getGameById(gameId: String): Game

    /**
     * Observes participant list updates for a match in real-time.
     */
    fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>>

    /**
     * Observes a single match document in real-time.
     */
    fun observeMatch(matchId: String): Flow<Result<Game>>
}
