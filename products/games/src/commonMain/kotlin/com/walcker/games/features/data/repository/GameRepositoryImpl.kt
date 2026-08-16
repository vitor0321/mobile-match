package com.walcker.games.features.data.repository

import com.walcker.games.features.data.cache.InMemoryMatchCache
import com.walcker.games.features.data.source.GameSource
import com.walcker.games.features.data.util.defaultShouldRetry
import com.walcker.games.features.data.util.withRetry
import com.walcker.games.features.domain.error.GamesError
import com.walcker.games.features.domain.error.toGamesError
import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.JoinMatchOutcome
import com.walcker.games.features.domain.model.LeaveMatchOutcome
import com.walcker.games.features.domain.model.MatchRole
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.repository.MyMatch
import kotlinx.coroutines.flow.Flow

internal class GameRepositoryImpl(
    private val source: GameSource,
    private val cache: InMemoryMatchCache,
) : GameRepository {

    override fun observeMatches(): Flow<List<Game>> = cache.matches

    override suspend fun refresh(): Result<Unit> {
        return runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.openGames()
            }
        }.mapCatching { games ->
            cache.replaceAll(games)
        }.recoverCatching { error ->
            throw error.toGamesError()
        }
    }

    /**
     * Returns the current cache contents without triggering a refresh.
     * Prefer [observeMatches] for reactive UI.
     */
    override suspend fun openGames(): Result<List<Game>> {
        return runCatching {
            cache.matches.value
        }.recoverCatching { error ->
            throw error.toGamesError()
        }
    }

    override suspend fun joinGame(gameId: String): Result<JoinMatchOutcome> {
        return runCatching { source.joinGame(gameId) }
            .recoverCatching { error -> throw error.toGamesError() }
    }

    override suspend fun createMatch(request: CreateMatchRequest): Result<String> {
        return runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.createMatch(request)
            }
        }.recoverCatching { error ->
            throw error.toGamesError()
        }
    }

    override suspend fun getMyMatches(userId: String): Result<List<MyMatch>> {
        return runCatching {
            source.matchesForUser(userId)
        }.map { games ->
            games.map { game ->
                val role = if (game.organizerId == userId) MatchRole.ORGANIZER else MatchRole.PARTICIPANT
                MyMatch(game = game, role = role)
            }
        }.recoverCatching { error ->
            throw error.toGamesError()
        }
    }

    override suspend fun cancelMatch(gameId: String): Result<CancelMatchOutcome> {
        return runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.cancelMatch(gameId)
            }
        }.recoverCatching { error -> throw error.toGamesError() }
    }

    override suspend fun leaveMatch(gameId: String): Result<LeaveMatchOutcome> {
        return runCatching {
            withRetry(shouldRetry = ::defaultShouldRetry) {
                source.leaveMatch(gameId)
            }
        }.recoverCatching { error -> throw error.toGamesError() }
    }

    override suspend fun getGameById(gameId: String): Result<Game> {
        return runCatching {
            source.getGameById(gameId)
        }.recoverCatching { error ->
            throw error.toGamesError()
        }
    }

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> {
        return source.observeParticipants(matchId)
    }

    override fun observeMatch(matchId: String): Flow<Result<Game>> {
        return source.observeMatch(matchId)
    }
}
