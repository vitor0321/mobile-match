package com.walcker.games.fake

import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.repository.RatingRepository

internal class FakeRatingRepository(
    var submitResult: Result<SubmitRatingOutcome> =
        Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 10)),
    var userRatingsResult: Result<List<Rating>> = Result.success(emptyList()),
    var matchLocationRatingsResult: Result<List<Rating>> = Result.success(emptyList()),
    var ratingsGivenForMatchResult: Result<List<Rating>> = Result.success(emptyList()),
    var mySkillRatingsResult: Result<Map<String, Int>> = Result.success(emptyMap()),
) : RatingRepository {
    val submitCalls: MutableList<String> = mutableListOf()

    override suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
    ): Result<SubmitRatingOutcome> {
        submitCalls += ratedUserId
        return submitResult
    }

    override suspend fun submitMatchRating(
        matchId: String,
        rating: Int,
    ): Result<SubmitRatingOutcome> {
        submitCalls += "match:$matchId"
        return submitResult
    }

    override suspend fun submitOrganizerRating(
        matchId: String,
        rating: Int,
    ): Result<SubmitRatingOutcome> {
        submitCalls += "organizer:$matchId"
        return submitResult
    }

    override suspend fun submitSkillRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
    ): Result<SubmitRatingOutcome> {
        submitCalls += "skill:$ratedUserId"
        return submitResult
    }

    override suspend fun getMySkillRatings(
        organizerId: String,
        userIds: List<String>,
    ): Result<Map<String, Int>> = mySkillRatingsResult

    override suspend fun getUserRatings(
        userId: String,
        limit: Int,
    ): Result<List<Rating>> = userRatingsResult

    override suspend fun getMatchLocationRatings(
        matchId: String,
        limit: Int,
    ): Result<List<Rating>> = matchLocationRatingsResult

    override suspend fun getRatingsGivenForMatch(
        matchId: String,
        raterUserId: String,
    ): Result<List<Rating>> = ratingsGivenForMatchResult
}
