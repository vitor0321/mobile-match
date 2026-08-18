package com.walcker.games.features.ui.player_search

import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.model.PlayerSearchFilters
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class PlayerSearchState(
    val query: String = "",
    val filters: PlayerSearchFilters = PlayerSearchFilters(),
    val results: ImmutableList<PlayerSearchResult> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFiltersPanel: Boolean = false,
)
