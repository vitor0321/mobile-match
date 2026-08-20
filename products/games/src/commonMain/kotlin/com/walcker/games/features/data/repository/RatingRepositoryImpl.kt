package com.walcker.games.features.data.repository

import com.walcker.games.features.data.cache.InMemoryPlayerCache
import com.walcker.games.features.data.source.RatingSource
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.features.domain.model.SubmitRatingOutcome
import com.walcker.games.features.domain.repository.RatingRepository

/**
 * Implementation of [RatingRepository].
 *
 * Delegates all operations to the data source layer.
 */
internal class RatingRepositoryImpl(
    private val ratingSource: RatingSource,
    private val playerCache: InMemoryPlayerCache,
) : RatingRepository {

    override suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome> =
        ratingSource.submitPlayerRating(matchId, ratedUserId, rating, comment, dimensions)
            // The function recomputed the rated player's average, so anything
            // cached about them — and any search ordered by rating — is stale.
            .onSuccess { playerCache.invalidatePlayer(ratedUserId) }

    override suspend fun getUserRatings(userId: String, limit: Int): Result<List<Rating>> =
        ratingSource.getUserRatings(userId, limit)

    override suspend fun getMatchLocationRatings(matchId: String, limit: Int): Result<List<Rating>> =
        ratingSource.getMatchLocationRatings(matchId, limit)
}
