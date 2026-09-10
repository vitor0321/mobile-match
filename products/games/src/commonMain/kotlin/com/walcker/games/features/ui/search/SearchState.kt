package com.walcker.games.features.ui.search

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.home.map.MapCamera
import com.walcker.games.strings.GameListStrings
import com.walcker.games.strings.MapStrings
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.games.strings.SearchStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal val DEFAULT_SEARCH_MAP_CAMERA = MapCamera(lat = -14.235, lng = -51.9253, zoom = 4f)
internal const val SEARCH_RESULTS_PAGE_SIZE = 20

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
    val mapStrings: MapStrings = PtBrGamesStrings.map,
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: ImmutableList<Game> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFiltersPanel: Boolean = false,
    val mySports: Set<Sport> = emptySet(),
    val showMap: Boolean = false,
    val selectedMapMatchId: String? = null,
    val visibleResultsCount: Int = SEARCH_RESULTS_PAGE_SIZE,
    val mapResults: ImmutableList<Game> = persistentListOf(),
    val mapCamera: MapCamera = DEFAULT_SEARCH_MAP_CAMERA,
    val isMapLoading: Boolean = false,
    val mapErrorMessage: String? = null,
) {
    val previewMatch: Game?
        get() = mapResults.find { it.id == selectedMapMatchId }

    val visibleResults: ImmutableList<Game>
        get() = if (results.size <= visibleResultsCount) results else results.take(visibleResultsCount).toImmutableList()

    val hasMoreResults: Boolean
        get() = results.size > visibleResultsCount
}
