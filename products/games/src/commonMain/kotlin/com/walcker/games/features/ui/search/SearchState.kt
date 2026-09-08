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

private val DEFAULT_SEARCH_CAMERA = MapCamera(lat = -14.235, lng = -51.9253, zoom = 4f)
private const val RESULT_CAMERA_ZOOM = 12f

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
) {
    val previewMatch: Game?
        get() = results.find { it.id == selectedMapMatchId }

    val mapCamera: MapCamera
        get() = results.firstOrNull()?.let { MapCamera(lat = it.lat, lng = it.lng, zoom = RESULT_CAMERA_ZOOM) } ?: DEFAULT_SEARCH_CAMERA
}
