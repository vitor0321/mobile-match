package com.walcker.games.features.domain.shared.model

internal sealed interface SubmitRatingOutcome {
    val averageRating: Float
    val ratingCount: Int

    data class Recorded(
        override val averageRating: Float,
        override val ratingCount: Int,
    ) : SubmitRatingOutcome

    data class AlreadyRated(
        override val averageRating: Float,
        override val ratingCount: Int,
    ) : SubmitRatingOutcome
}
