package com.walcker.games.features.ui.player_search

import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class PlayerSearchState(
    val query: String = "",
    val filters: PlayerSearchFilters = PlayerSearchFilters(),
    val results: ImmutableList<PlayerSearchResult> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFiltersPanel: Boolean = false,
    /**
     * The search read as many profiles as it is allowed to, so matching players
     * may exist beyond them. Shown as a hint to narrow the search — pretending
     * the list is complete would be the actual bug.
     */
    val reachedLimit: Boolean = false,
) {
    /** True while nothing has been typed and no filter is active. */
    val isIdle: Boolean
        get() = query.isBlank() && filters.withoutQuery() == PlayerSearchFilters()
}

internal fun PlayerSearchFilters.withoutQuery(): PlayerSearchFilters = copy(query = "")
