package com.walcker.games.features.data.repository

import com.walcker.games.features.data.cache.InMemoryMatchCache
import com.walcker.games.features.data.source.GameSource
import com.walcker.games.features.data.util.defaultShouldRetry
import com.walcker.games.features.data.util.withRetry
import com.walcker.games.features.domain.error.toGamesError
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.repository.GameRepository
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

    override suspend fun joinGame(gameId: String): Result<Unit> {
        return runCatching { source.joinGame(gameId) }
            .onFailure { error -> throw error.toGamesError() }
    }
}
