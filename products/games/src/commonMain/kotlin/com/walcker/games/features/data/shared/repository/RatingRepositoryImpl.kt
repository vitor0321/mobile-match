package com.walcker.games.features.data.shared.repository

import com.walcker.games.features.data.shared.cache.InMemoryPlayerCache
import com.walcker.games.features.data.shared.source.RatingSource
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.RatingDimensions
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.repository.RatingRepository

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
        ratingSource
            .submitPlayerRating(matchId, ratedUserId, rating, comment, dimensions)
            .onSuccess { playerCache.invalidatePlayer(ratedUserId) }

    override suspend fun submitMatchRating(
        matchId: String,
        rating: Int,
    ): Result<SubmitRatingOutcome> = ratingSource.submitMatchRating(matchId, rating)

    override suspend fun getUserRatings(
        userId: String,
        limit: Int,
    ): Result<List<Rating>> = ratingSource.getUserRatings(userId, limit)

    override suspend fun getMatchLocationRatings(
        matchId: String,
        limit: Int,
    ): Result<List<Rating>> = ratingSource.getMatchLocationRatings(matchId, limit)
}
