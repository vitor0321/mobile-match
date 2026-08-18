package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResults
import com.walcker.games.features.domain.repository.PlayerRepository

/**
 * Use case for searching players with advanced filters.
 *
 * Delegates to [PlayerRepository], which caches and decides what runs on the
 * server versus in memory (see `FirestorePlayerSource`).
 */
internal interface SearchPlayersUseCase {
    suspend operator fun invoke(filters: PlayerSearchFilters): Result<PlayerSearchResults>
}

internal class SearchPlayersUseCaseImpl(
    private val repository: PlayerRepository,
) : SearchPlayersUseCase {
    override suspend fun invoke(filters: PlayerSearchFilters): Result<PlayerSearchResults> {
        return repository.searchPlayers(filters)
    }
}
