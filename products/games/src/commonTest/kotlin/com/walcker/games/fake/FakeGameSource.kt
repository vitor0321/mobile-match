package com.walcker.games.fake

import com.walcker.games.features.data.shared.source.GameSource
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeGameSource(
    var openGamesResult: () -> List<Game> = { listOf(game()) },
    var joinGameResult: () -> JoinMatchOutcome = { JoinMatchOutcome.Confirmed(matchId = "match-1") },
    var leaveMatchResult: () -> LeaveMatchOutcome = { LeaveMatchOutcome(matchId = "match-1") },
    var cancelMatchResult: () -> CancelMatchOutcome = { CancelMatchOutcome.Cancelled(matchId = "match-1") },
    var createMatchResult: () -> String = { "match-1" },
    var updateMatchResult: () -> Unit = {},
    var matchesForUserResult: () -> List<Game> = { emptyList() },
    var getGameByIdResult: () -> Game = { game() },
) : GameSource {
    var openGamesCallCount: Int = 0
        private set
    var joinGameCallCount: Int = 0
        private set
    var createMatchCallCount: Int = 0
        private set
    var updateMatchCallCount: Int = 0
        private set
    var cancelMatchCallCount: Int = 0
        private set
    var cancelMatchSeriesCallCount: Int = 0
        private set
    var leaveMatchCallCount: Int = 0
        private set

    override suspend fun openGames(radiusKm: Double): List<Game> {
        openGamesCallCount++
        return openGamesResult()
    }

    override suspend fun joinGame(gameId: String): JoinMatchOutcome {
        joinGameCallCount++
        return joinGameResult()
    }

    override suspend fun leaveMatch(gameId: String): LeaveMatchOutcome {
        leaveMatchCallCount++
        return leaveMatchResult()
    }

    override suspend fun cancelMatch(gameId: String): CancelMatchOutcome {
        cancelMatchCallCount++
        return cancelMatchResult()
    }

    override suspend fun cancelMatchSeries(matchId: String) {
        cancelMatchSeriesCallCount++
    }

    override suspend fun createMatch(request: CreateMatchRequest): String {
        createMatchCallCount++
        return createMatchResult()
    }

    override suspend fun updateMatch(
        matchId: String,
        request: CreateMatchRequest,
    ) {
        updateMatchCallCount++
        updateMatchResult()
    }

    override suspend fun matchesForUser(userId: String): List<Game> = matchesForUserResult()

    override suspend fun getGameById(gameId: String): Game = getGameByIdResult()

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> = emptyFlow()

    override fun observeMatch(matchId: String): Flow<Result<Game>> = emptyFlow()
}
