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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

internal class FakeGameRepository(
    var myMatches: Result<List<MyMatch>> = Result.success(emptyList()),
    var refreshResult: Result<Unit> = Result.success(Unit),
    var joinGameResult: Result<JoinMatchOutcome> = Result.success(JoinMatchOutcome.Confirmed(matchId = "match-1")),
    var createMatchResult: Result<String> = Result.success("match-1"),
    var updateMatchResult: Result<Unit> = Result.success(Unit),
    var cancelMatchResult: Result<CancelMatchOutcome> = Result.success(CancelMatchOutcome.Cancelled("match-1")),
    var leaveMatchResult: Result<LeaveMatchOutcome> = Result.success(LeaveMatchOutcome("match-1")),
    var getGameByIdResult: Result<Game> = Result.success(game()),
) : GameRepository {
    private val matchesFlow = MutableStateFlow<List<Game>>(emptyList())
    private val participantsFlow = MutableStateFlow<Result<ParticipantsSummary>?>(null)
    private val matchFlow = MutableStateFlow<Result<Game>?>(null)

    val refreshCalls: MutableList<Double> = mutableListOf()
    val joinGameCalls: MutableList<String> = mutableListOf()
    val createMatchCalls: MutableList<CreateMatchRequest> = mutableListOf()
    val updateMatchCalls: MutableList<Pair<String, CreateMatchRequest>> = mutableListOf()
    val cancelMatchCalls: MutableList<String> = mutableListOf()
    val leaveMatchCalls: MutableList<String> = mutableListOf()
    val getGameByIdCalls: MutableList<String> = mutableListOf()

    fun emitMatches(games: List<Game>) {
        matchesFlow.value = games
    }

    fun emitParticipants(result: Result<ParticipantsSummary>) {
        participantsFlow.value = result
    }

    fun emitMatch(result: Result<Game>) {
        matchFlow.value = result
    }

    override fun observeMatches(): Flow<List<Game>> = matchesFlow.asStateFlow()

    override suspend fun refresh(radiusKm: Double): Result<Unit> {
        refreshCalls += radiusKm
        return refreshResult
    }

    override suspend fun joinGame(gameId: String): Result<JoinMatchOutcome> {
        joinGameCalls += gameId
        return joinGameResult
    }

    override suspend fun createMatch(request: CreateMatchRequest): Result<String> {
        createMatchCalls += request
        return createMatchResult
    }

    override suspend fun updateMatch(
        matchId: String,
        request: CreateMatchRequest,
    ): Result<Unit> {
        updateMatchCalls += matchId to request
        return updateMatchResult
    }

    override suspend fun getMyMatches(userId: String): Result<List<MyMatch>> = myMatches

    override suspend fun cancelMatch(gameId: String): Result<CancelMatchOutcome> {
        cancelMatchCalls += gameId
        return cancelMatchResult
    }

    override suspend fun leaveMatch(gameId: String): Result<LeaveMatchOutcome> {
        leaveMatchCalls += gameId
        return leaveMatchResult
    }

    override suspend fun getGameById(gameId: String): Result<Game> {
        getGameByIdCalls += gameId
        return getGameByIdResult
    }

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> =
        flow {
            participantsFlow.collect { value -> if (value != null) emit(value) }
        }

    override fun observeMatch(matchId: String): Flow<Result<Game>> =
        flow {
            matchFlow.collect { value -> if (value != null) emit(value) }
        }
}
