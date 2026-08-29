package com.walcker.games.features.ui.search

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.strings.GameListStrings
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.games.strings.SearchStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SearchFilters(
    val startDateMs: Long? = null,
    val endDateMs: Long? = null,
    val sports: Set<Sport> = emptySet(),
    val minPrice: Float? = null,
    val maxPrice: Float? = null,
)

internal data class SearchState(
    val strings: SearchStrings = PtBrGamesStrings.search,
    val cardStrings: GameListStrings = PtBrGamesStrings.gameList,
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: ImmutableList<Game> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFiltersPanel: Boolean = false,
)
