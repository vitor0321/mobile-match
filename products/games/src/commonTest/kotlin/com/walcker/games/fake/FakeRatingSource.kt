package com.walcker.games.fake

import com.walcker.games.features.data.shared.source.RatingSource
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.RatingDimensions
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome

internal class FakeRatingSource(
    var submitResult: Result<SubmitRatingOutcome> =
        Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 10)),
    var userRatingsResult: Result<List<Rating>> = Result.success(emptyList()),
    var matchLocationRatingsResult: Result<List<Rating>> = Result.success(emptyList()),
) : RatingSource {
    var submitCallCount: Int = 0
        private set

    override suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome> {
        submitCallCount++
        return submitResult
    }

    override suspend fun getUserRatings(
        userId: String,
        limit: Int,
    ): Result<List<Rating>> = userRatingsResult

    override suspend fun getUserRatingsPage(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> = Result.success(RatingsPage.Empty)

    override suspend fun getMatchLocationRatings(
        matchId: String,
        limit: Int,
    ): Result<List<Rating>> = matchLocationRatingsResult
}
