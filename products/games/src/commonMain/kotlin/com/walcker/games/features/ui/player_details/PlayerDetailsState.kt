package com.walcker.games.features.ui.player_details

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingDistribution
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class PlayerDetailsState(
    val userId: String = "",
    val player: PlayerDetails? = null,
    val previewRatings: ImmutableList<Rating> = persistentListOf(),
    val distribution: RatingDistribution = RatingDistribution.Empty,
    val hasMoreRatings: Boolean = false,
    val isLoadingPlayer: Boolean = false,
    val isLoadingRatings: Boolean = false,
    val errorMessage: String? = null,
) {
    internal companion object {
        internal const val PREVIEW_RATINGS_COUNT: Int = 5

        internal const val RATINGS_SAMPLE_SIZE: Int = 20
    }
}
