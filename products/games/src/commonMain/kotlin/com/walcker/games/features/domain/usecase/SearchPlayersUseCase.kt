package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.repository.PlayerRepository

/**
 * Use case for searching players with advanced filters.
 *
 * Delegates to [PlayerRepository] which handles Firestore queries.
 * All filtering logic (name, rating, sports, min matches) is implemented
 * by the repository to support server-side filtering where possible.
 */
internal interface SearchPlayersUseCase {
    suspend operator fun invoke(filters: PlayerSearchFilters): Result<List<PlayerSearchResult>>
}

internal class SearchPlayersUseCaseImpl(
    private val repository: PlayerRepository,
) : SearchPlayersUseCase {
    override suspend fun invoke(filters: PlayerSearchFilters): Result<List<PlayerSearchResult>> {
        return repository.searchPlayers(filters)
    }
}
