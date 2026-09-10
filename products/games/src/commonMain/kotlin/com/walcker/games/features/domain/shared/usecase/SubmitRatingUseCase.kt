package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.repository.RatingRepository

internal class SubmitRatingUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
    ): Result<SubmitRatingOutcome> = ratingRepository.submitPlayerRating(matchId, ratedUserId, rating, comment)
}

internal class SubmitMatchRatingUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(
        matchId: String,
        rating: Int,
    ): Result<SubmitRatingOutcome> = ratingRepository.submitMatchRating(matchId, rating)
}

internal class SubmitOrganizerRatingUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(
        matchId: String,
        rating: Int,
    ): Result<SubmitRatingOutcome> = ratingRepository.submitOrganizerRating(matchId, rating)
}

internal class GetUserRatingsUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(
        userId: String,
        limit: Int = 50,
    ) = ratingRepository.getUserRatings(userId, limit)
}
