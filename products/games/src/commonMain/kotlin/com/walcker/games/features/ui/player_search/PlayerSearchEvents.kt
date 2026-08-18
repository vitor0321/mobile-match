package com.walcker.games.features.ui.player_search

import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.Sport

internal sealed interface PlayerSearchEvents {
    data class QueryChanged(val query: String) : PlayerSearchEvents
    data class FiltersChanged(val filters: PlayerSearchFilters) : PlayerSearchEvents
    data class MinRatingChanged(val minRating: Float?) : PlayerSearchEvents
    data class MaxRatingChanged(val maxRating: Float?) : PlayerSearchEvents
    data class SportsFilterChanged(val sports: Set<Sport>) : PlayerSearchEvents
    data object ResetFilters : PlayerSearchEvents
    data object ToggleFiltersPanel : PlayerSearchEvents
    data class SelectPlayer(val userId: String) : PlayerSearchEvents
}

internal sealed interface PlayerSearchEffect {
    data class ShowMessage(val message: String) : PlayerSearchEffect
    data class NavigateToPlayer(val userId: String) : PlayerSearchEffect
}
