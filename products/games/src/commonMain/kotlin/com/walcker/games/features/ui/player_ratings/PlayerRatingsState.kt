package com.walcker.games.features.ui.player_ratings

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingSort
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class PlayerRatingsState(
    val userId: String = "",
    val playerName: String = "",
    val ratings: ImmutableList<Rating> = persistentListOf(),
    val sort: RatingSort = RatingSort.RECENT,
    val isLoadingFirstPage: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = ratings.isEmpty() && !isLoadingFirstPage && errorMessage == null
}
