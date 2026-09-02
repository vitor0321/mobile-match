package com.walcker.games.fake

import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.RatingDimensions
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.repository.RatingRepository

internal class FakeRatingRepository(
    var submitResult: Result<SubmitRatingOutcome> =
        Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 10)),
    var userRatingsResult: Result<List<Rating>> = Result.success(emptyList()),
    var matchLocationRatingsResult: Result<List<Rating>> = Result.success(emptyList()),
) : RatingRepository {
    val submitCalls: MutableList<String> = mutableListOf()

    override suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome> {
        submitCalls += ratedUserId
        return submitResult
    }

    override suspend fun getUserRatings(
        userId: String,
        limit: Int,
    ): Result<List<Rating>> = userRatingsResult

    override suspend fun getMatchLocationRatings(
        matchId: String,
        limit: Int,
    ): Result<List<Rating>> = matchLocationRatingsResult
}
