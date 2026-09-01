package com.walcker.games.fake

import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.features.domain.shared.repository.MyMatch
import kotlinx.coroutines.flow.Flow

internal class FakeGameRepository(
    var myMatches: Result<List<MyMatch>> = Result.success(emptyList()),
) : GameRepository {
    override suspend fun getMyMatches(userId: String): Result<List<MyMatch>> = myMatches

    override fun observeMatches(): Flow<List<Game>> = notUsed()

    override suspend fun refresh(radiusKm: Double): Result<Unit> = notUsed()

    override suspend fun joinGame(gameId: String): Result<JoinMatchOutcome> = notUsed()

    override suspend fun createMatch(request: CreateMatchRequest): Result<String> = notUsed()

    override suspend fun updateMatch(
        matchId: String,
        request: CreateMatchRequest,
    ): Result<Unit> = notUsed()

    override suspend fun cancelMatch(gameId: String): Result<CancelMatchOutcome> = notUsed()

    override suspend fun leaveMatch(gameId: String): Result<LeaveMatchOutcome> = notUsed()

    override suspend fun getGameById(gameId: String): Result<Game> = notUsed()

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> = notUsed()

    override fun observeMatch(matchId: String): Flow<Result<Game>> = notUsed()

    private fun notUsed(): Nothing = error("FakeGameRepository: método não usado por este teste")
}
