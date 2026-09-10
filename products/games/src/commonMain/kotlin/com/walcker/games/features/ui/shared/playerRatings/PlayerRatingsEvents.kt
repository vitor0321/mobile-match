package com.walcker.games.features.ui.shared.playerRatings

import com.walcker.games.features.domain.shared.model.RatingSort

internal sealed interface PlayerRatingsEvents {
    data object Retry : PlayerRatingsEvents

    data object LoadNextPage : PlayerRatingsEvents

    data class SortChanged(
        val sort: RatingSort,
    ) : PlayerRatingsEvents
}

internal sealed interface PlayerRatingsEffect {
    data class ShowMessage(
        val message: String,
    ) : PlayerRatingsEffect
}
