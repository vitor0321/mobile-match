package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.features.domain.model.SubmitRatingOutcome
import com.walcker.games.features.domain.repository.RatingRepository

internal class SubmitRatingUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome> =
        ratingRepository.submitPlayerRating(matchId, ratedUserId, rating, comment, dimensions)
}

internal class GetUserRatingsUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(userId: String, limit: Int = 50) =
        ratingRepository.getUserRatings(userId, limit)
}
