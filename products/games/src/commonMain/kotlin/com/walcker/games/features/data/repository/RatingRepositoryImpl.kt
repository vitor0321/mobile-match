package com.walcker.games.features.data.repository

import com.walcker.games.features.data.source.RatingSource
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.repository.RatingRepository

/**
 * Implementation of [RatingRepository].
 *
 * Delegates all operations to the data source layer.
 */
internal class RatingRepositoryImpl(
    private val ratingSource: RatingSource,
) : RatingRepository {

    override suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
    ): Result<Unit> = ratingSource.submitPlayerRating(matchId, ratedUserId, rating, comment)

    override suspend fun getUserRatings(userId: String, limit: Int): Result<List<Rating>> =
        ratingSource.getUserRatings(userId, limit)

    override suspend fun getMatchLocationRatings(matchId: String, limit: Int): Result<List<Rating>> =
        ratingSource.getMatchLocationRatings(matchId, limit)
}
