package com.walcker.games.fake

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.games.features.domain.repository.PlayerRepository

/**
 * Hand-written fake instead of a mock: the assertions here are about what the
 * step models do with the results, and a fake keeps that readable.
 *
 * [ratingPages] is keyed by the cursor the caller sent (`null` for the first
 * page), so a test can describe a whole pagination sequence declaratively.
 */
internal class FakePlayerRepository(
    var searchResult: Result<List<PlayerSearchResult>> = Result.success(emptyList()),
    var detailsResult: Result<PlayerDetails> = Result.success(playerDetails()),
    var ratingPages: Map<String?, Result<RatingsPage>> =
        mapOf(null to Result.success(RatingsPage.Empty)),
) : PlayerRepository {

    /** Every `getPlayerRatings` call, in order — lets tests assert the query. */
    val ratingCalls: MutableList<RatingCall> = mutableListOf()

    data class RatingCall(
        val userId: String,
        val limit: Int,
        val sort: RatingSort,
        val cursor: String?,
    )

    override suspend fun searchPlayers(
        filters: PlayerSearchFilters,
    ): Result<List<PlayerSearchResult>> = searchResult

    override suspend fun getPlayerDetails(userId: String): Result<PlayerDetails> = detailsResult

    override suspend fun getPlayerRatings(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> {
        ratingCalls += RatingCall(userId = userId, limit = limit, sort = sort, cursor = cursor)
        return ratingPages[cursor] ?: Result.success(RatingsPage.Empty)
    }
}

internal fun playerDetails(
    userId: String = "player-1",
    displayName: String = "Ana Souza",
    averageRating: Float = 4.5f,
    totalRatings: Int = 12,
): PlayerDetails = PlayerDetails(
    userId = userId,
    displayName = displayName,
    photoUrl = null,
    email = "ana@match.app",
    bio = "Meia-armadora",
    averageRating = averageRating,
    totalRatings = totalRatings,
    matchesOrganized = 3,
    matchesParticipated = 21,
    favoriteSports = emptyList(),
    city = "Porto Alegre",
    neighborhood = "Menino Deus",
    locationRadius = 15,
    joinRate = 0.9f,
    cancelRate = 0.05f,
    memberSince = 1_735_689_600L,
)

internal fun rating(
    id: String,
    stars: Int = 5,
    comment: String = "Jogou muito",
    createdAtMs: Long = 1_760_000_000_000L,
): Rating = Rating(
    id = id,
    matchId = "match-1",
    ratedUserId = "player-1",
    raterUserId = "player-2",
    rating = stars,
    comment = comment,
    createdAtMs = createdAtMs,
)
