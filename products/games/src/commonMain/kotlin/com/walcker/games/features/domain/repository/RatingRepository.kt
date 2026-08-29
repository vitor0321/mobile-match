package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.features.domain.model.SubmitRatingOutcome

internal interface RatingRepository {
    suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome>

    suspend fun getUserRatings(userId: String, limit: Int = 50): Result<List<Rating>>

    suspend fun getMatchLocationRatings(matchId: String, limit: Int = 10): Result<List<Rating>>
}
