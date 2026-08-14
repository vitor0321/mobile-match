package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.Game
import kotlinx.coroutines.flow.Flow

internal interface GameRepository {
    /**
     * Reactive stream of cached matches.
     *
     * Always emits the current cache contents, then re-emits whenever
     * [refresh] succeeds. UI should observe this rather than calling
     * [openGames] directly so it benefits from the offline-first cache.
     */
    fun observeMatches(): Flow<List<Game>>

    /**
     * Triggers a network refresh of the match list.
     *
     * On success, updates the cache and returns [Result.success].
     * On failure, returns [Result.failure] with a [com.walcker.games.features.domain.error.GamesError].
     */
    suspend fun refresh(): Result<Unit>

    /**
     * @deprecated use [observeMatches] for reactive UI. Kept for callers that
     * want a one-shot read of the current cache.
     */
    suspend fun openGames(): Result<List<Game>>

    suspend fun joinGame(gameId: String): Result<Unit>
}
