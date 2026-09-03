package com.walcker.games.features.domain.shared.repository

import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import kotlinx.coroutines.flow.Flow

internal data class MyMatch(
    val game: Game,
    val role: MatchRole,
)

internal interface GameRepository {
    fun observeMatches(): Flow<List<Game>>

    suspend fun refresh(radiusKm: Double): Result<Unit>

    suspend fun joinGame(gameId: String): Result<JoinMatchOutcome>

    suspend fun createMatch(request: CreateMatchRequest): Result<String>

    suspend fun updateMatch(
        matchId: String,
        request: CreateMatchRequest,
    ): Result<Unit>

    suspend fun getMyMatches(userId: String): Result<List<MyMatch>>

    suspend fun cancelMatch(gameId: String): Result<CancelMatchOutcome>

    suspend fun cancelMatchSeries(matchId: String): Result<Unit>

    suspend fun leaveMatch(gameId: String): Result<LeaveMatchOutcome>

    suspend fun getGameById(gameId: String): Result<Game>

    fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>>

    fun observeMatch(matchId: String): Flow<Result<Game>>
}
