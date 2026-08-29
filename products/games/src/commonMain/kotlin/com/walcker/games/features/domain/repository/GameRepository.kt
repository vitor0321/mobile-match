package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.JoinMatchOutcome
import com.walcker.games.features.domain.model.LeaveMatchOutcome
import com.walcker.games.features.domain.model.MatchRole
import com.walcker.games.features.domain.model.ParticipantsSummary
import kotlinx.coroutines.flow.Flow

internal data class MyMatch(
    val game: Game,
    val role: MatchRole,
)

internal interface GameRepository {
    fun observeMatches(): Flow<List<Game>>

    suspend fun refresh(): Result<Unit>

    suspend fun openGames(): Result<List<Game>>

    suspend fun joinGame(gameId: String): Result<JoinMatchOutcome>

    suspend fun createMatch(request: CreateMatchRequest): Result<String>

    suspend fun getMyMatches(userId: String): Result<List<MyMatch>>

    suspend fun cancelMatch(gameId: String): Result<CancelMatchOutcome>

    suspend fun leaveMatch(gameId: String): Result<LeaveMatchOutcome>

    suspend fun getGameById(gameId: String): Result<Game>

    fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>>

    fun observeMatch(matchId: String): Flow<Result<Game>>
}
