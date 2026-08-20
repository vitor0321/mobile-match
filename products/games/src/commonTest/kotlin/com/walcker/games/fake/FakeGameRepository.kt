package com.walcker.games.fake

import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.JoinMatchOutcome
import com.walcker.games.features.domain.model.LeaveMatchOutcome
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.repository.MyMatch
import kotlinx.coroutines.flow.Flow

/**
 * Fake escrito à mão em vez de mock: só `getMyMatches` é usado hoje, e deixar o
 * resto explodindo deixa claro para o próximo teste o que ainda não foi coberto.
 */
internal class FakeGameRepository(
    var myMatches: Result<List<MyMatch>> = Result.success(emptyList()),
) : GameRepository {

    override suspend fun getMyMatches(userId: String): Result<List<MyMatch>> = myMatches

    override fun observeMatches(): Flow<List<Game>> = notUsed()
    override suspend fun refresh(): Result<Unit> = notUsed()
    override suspend fun openGames(): Result<List<Game>> = notUsed()
    override suspend fun joinGame(gameId: String): Result<JoinMatchOutcome> = notUsed()
    override suspend fun createMatch(request: CreateMatchRequest): Result<String> = notUsed()
    override suspend fun cancelMatch(gameId: String): Result<CancelMatchOutcome> = notUsed()
    override suspend fun leaveMatch(gameId: String): Result<LeaveMatchOutcome> = notUsed()
    override suspend fun getGameById(gameId: String): Result<Game> = notUsed()
    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> = notUsed()
    override fun observeMatch(matchId: String): Flow<Result<Game>> = notUsed()

    private fun notUsed(): Nothing =
        error("FakeGameRepository: método não usado por este teste")
}
