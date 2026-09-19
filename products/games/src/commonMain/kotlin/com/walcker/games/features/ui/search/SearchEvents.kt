package com.walcker.games.features.ui.search

import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.home.map.MapCamera

internal sealed interface SearchEvents {
    data class QueryChanged(
        val query: String,
    ) : SearchEvents

    data class DateRangeChanged(
        val startDateMs: Long?,
        val endDateMs: Long?,
    ) : SearchEvents

    data class SportFilterChanged(
        val sports: Set<Sport>,
    ) : SearchEvents

    data class PriceRangeChanged(
        val minPrice: Float?,
        val maxPrice: Float?,
    ) : SearchEvents

    data object ResetFilters : SearchEvents

    data object ToggleFiltersPanel : SearchEvents

    data class SelectGame(
        val gameId: String,
    ) : SearchEvents

    data object Retry : SearchEvents

    data object ToggleMap : SearchEvents

    data class PinSelected(
        val matchId: String,
    ) : SearchEvents

    data object MapPreviewDismissed : SearchEvents

    data object LoadMoreResults : SearchEvents

    data class MapCameraIdle(
        val camera: MapCamera,
    ) : SearchEvents

    data object MapRetry : SearchEvents

    data object MapErrorDismissed : SearchEvents
}

internal sealed interface SearchEffect {
    data class ShowMessage(
        val message: String,
    ) : SearchEffect

    data class NavigateToMatchDetail(
        val matchId: String,
    ) : SearchEffect
}
