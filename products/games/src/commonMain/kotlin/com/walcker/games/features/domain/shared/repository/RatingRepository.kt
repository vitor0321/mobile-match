package com.walcker.games.features.domain.shared.repository

import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.RatingDimensions
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome

internal interface RatingRepository {
    suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome>

    suspend fun submitMatchRating(
        matchId: String,
        rating: Int,
    ): Result<SubmitRatingOutcome>

    suspend fun getUserRatings(
        userId: String,
        limit: Int = 50,
    ): Result<List<Rating>>

    suspend fun getMatchLocationRatings(
        matchId: String,
        limit: Int = 10,
    ): Result<List<Rating>>
}
