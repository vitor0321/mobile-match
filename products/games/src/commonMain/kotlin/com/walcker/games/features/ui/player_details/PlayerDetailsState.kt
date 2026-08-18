package com.walcker.games.features.ui.player_details

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingDistribution
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Everything the player details screen needs to render.
 *
 * [previewRatings] and [distribution] are derived once in the step model rather
 * than in a `get()` here: a computed property would rebuild both lists on every
 * recomposition and defeat Compose's equality checks.
 */
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
        /** Reviews shown inline before the user has to open the full list. */
        internal const val PREVIEW_RATINGS_COUNT: Int = 5

        /**
         * Sample fetched for the details screen. Larger than what is displayed
         * because the distribution histogram is computed client-side (roadmap
         * decision D10) and a 5-item sample would make it meaningless.
         */
        internal const val RATINGS_SAMPLE_SIZE: Int = 20
    }
}
