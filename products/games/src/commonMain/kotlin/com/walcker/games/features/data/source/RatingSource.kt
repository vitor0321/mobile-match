package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.games.features.domain.model.SubmitRatingOutcome

internal interface RatingSource {
    suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome>

    suspend fun getUserRatings(userId: String, limit: Int): Result<List<Rating>>

    suspend fun getUserRatingsPage(
        userId: String,
        limit: Int,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>

    suspend fun getMatchLocationRatings(matchId: String, limit: Int): Result<List<Rating>>
}
