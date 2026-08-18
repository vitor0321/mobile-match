package com.walcker.games.features.ui.search

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Sport
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Filters for advanced game search.
 *
 * @param startDateMs Search from this date (milliseconds since epoch). Null = no minimum.
 * @param endDateMs Search until this date (milliseconds since epoch). Null = no maximum.
 * @param sports Selected sports to filter by. Empty = all sports.
 * @param minPrice Minimum price filter (null = no minimum).
 * @param maxPrice Maximum price filter (null = no maximum).
 */
internal data class SearchFilters(
    val startDateMs: Long? = null,
    val endDateMs: Long? = null,
    val sports: Set<Sport> = emptySet(),
    val minPrice: Float? = null,
    val maxPrice: Float? = null,
)

internal data class SearchState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: ImmutableList<Game> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFiltersPanel: Boolean = false,
)
