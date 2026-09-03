package com.walcker.games.features.data.shared.source

import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.NearbyMatchesPage
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import kotlinx.coroutines.flow.Flow

internal interface GameSource {
    suspend fun openGames(
        radiusKm: Double,
        cursors: List<String?>? = null,
    ): NearbyMatchesPage

    suspend fun joinGame(gameId: String): JoinMatchOutcome

    suspend fun leaveMatch(gameId: String): LeaveMatchOutcome

    suspend fun cancelMatch(gameId: String): CancelMatchOutcome

    suspend fun cancelMatchSeries(matchId: String)

    suspend fun createMatch(request: CreateMatchRequest): String

    suspend fun updateMatch(
        matchId: String,
        request: CreateMatchRequest,
    )

    suspend fun matchesForUser(userId: String): List<Game>

    suspend fun getGameById(gameId: String): Game

    fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>>

    fun observeMatch(matchId: String): Flow<Result<Game>>
}
