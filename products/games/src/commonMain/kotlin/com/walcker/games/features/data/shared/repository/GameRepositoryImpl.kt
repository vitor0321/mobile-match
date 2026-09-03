package com.walcker.games.features.data.shared.repository

import com.walcker.games.features.data.shared.cache.InMemoryMatchCache
import com.walcker.games.features.data.shared.source.GameSource
import com.walcker.games.features.data.shared.util.defaultShouldRetry
import com.walcker.games.features.data.shared.util.withRetry
import com.walcker.games.features.domain.shared.error.toGamesError
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.features.domain.shared.repository.MyMatch
import kotlinx.coroutines.flow.Flow

internal class GameRepositoryImpl(
    private val source: GameSource,
    private val cache: InMemoryMatchCache,
) : GameRepository {
    override fun observeMatches(): Flow<List<Game>> = cache.matches

    override suspend fun refresh(radiusKm: Double): Result<Unit> =
        runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.openGames(radiusKm)
            }
        }.mapCatching { games ->
            cache.replaceAll(games)
        }.recoverCatching { error ->
            throw error.toGamesError()
        }

    override suspend fun joinGame(gameId: String): Result<JoinMatchOutcome> =
        runCatching { source.joinGame(gameId) }
            .recoverCatching { error -> throw error.toGamesError() }

    override suspend fun createMatch(request: CreateMatchRequest): Result<String> =
        runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.createMatch(request)
            }
        }.recoverCatching { error ->
            throw error.toGamesError()
        }

    override suspend fun updateMatch(
        matchId: String,
        request: CreateMatchRequest,
    ): Result<Unit> =
        runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.updateMatch(matchId, request)
            }
        }.recoverCatching { error ->
            throw error.toGamesError()
        }

    override suspend fun getMyMatches(userId: String): Result<List<MyMatch>> =
        runCatching {
            source.matchesForUser(userId)
        }.map { games ->
            games.map { game ->
                val role = if (game.organizerId == userId) MatchRole.ORGANIZER else MatchRole.PARTICIPANT
                MyMatch(game = game, role = role)
            }
        }.recoverCatching { error ->
            throw error.toGamesError()
        }

    override suspend fun cancelMatch(gameId: String): Result<CancelMatchOutcome> =
        runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.cancelMatch(gameId)
            }
        }.recoverCatching { error -> throw error.toGamesError() }

    override suspend fun cancelMatchSeries(matchId: String): Result<Unit> =
        runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.cancelMatchSeries(matchId)
            }
        }.recoverCatching { error -> throw error.toGamesError() }

    override suspend fun leaveMatch(gameId: String): Result<LeaveMatchOutcome> =
        runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.leaveMatch(gameId)
            }
        }.recoverCatching { error -> throw error.toGamesError() }

    override suspend fun getGameById(gameId: String): Result<Game> =
        runCatching {
            source.getGameById(gameId)
        }.recoverCatching { error ->
            throw error.toGamesError()
        }

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> = source.observeParticipants(matchId)

    override fun observeMatch(matchId: String): Flow<Result<Game>> = source.observeMatch(matchId)
}
