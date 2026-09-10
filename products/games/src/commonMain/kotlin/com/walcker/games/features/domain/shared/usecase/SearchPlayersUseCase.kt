package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.PlayerSearchResults
import com.walcker.games.features.domain.shared.repository.PlayerRepository

internal interface SearchPlayersUseCase {
    suspend operator fun invoke(filters: PlayerSearchFilters): Result<PlayerSearchResults>
}

internal class SearchPlayersUseCaseImpl(
    private val repository: PlayerRepository,
) : SearchPlayersUseCase {
    override suspend fun invoke(filters: PlayerSearchFilters): Result<PlayerSearchResults> = repository.searchPlayers(filters)
}
