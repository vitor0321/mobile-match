package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.JoinMatchOutcome
import com.walcker.games.features.domain.model.LeaveMatchOutcome
import com.walcker.games.features.domain.model.ParticipantsSummary
import kotlinx.coroutines.flow.Flow

internal interface GameSource {
    suspend fun openGames(): List<Game>
    suspend fun joinGame(gameId: String): JoinMatchOutcome
    suspend fun leaveMatch(gameId: String): LeaveMatchOutcome
    suspend fun cancelMatch(gameId: String): CancelMatchOutcome
    suspend fun createMatch(request: CreateMatchRequest): String

    suspend fun matchesForUser(userId: String): List<Game>

    suspend fun getGameById(gameId: String): Game

    fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>>

    fun observeMatch(matchId: String): Flow<Result<Game>>
}
