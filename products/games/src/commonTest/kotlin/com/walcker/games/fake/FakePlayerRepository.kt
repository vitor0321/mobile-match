package com.walcker.games.fake

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.model.PlayerSearchResults
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.features.domain.repository.PlayerRepository

internal class FakePlayerRepository(
    var searchResult: Result<PlayerSearchResults> = Result.success(PlayerSearchResults.Empty),
    var detailsResult: Result<PlayerDetails> = Result.success(playerDetails()),
    var ratingPages: Map<String?, Result<RatingsPage>> =
        mapOf(null to Result.success(RatingsPage.Empty)),
) : PlayerRepository {

    val searchCalls: MutableList<PlayerSearchFilters> = mutableListOf()

    val ratingCalls: MutableList<RatingCall> = mutableListOf()

    data class RatingCall(
        val userId: String,
        val limit: Int,
        val sort: RatingSort,
        val cursor: String?,
    )

    override suspend fun searchPlayers(
        filters: PlayerSearchFilters,
    ): Result<PlayerSearchResults> {
        searchCalls += filters
        return searchResult
    }

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
    averageRating = averageRating,
    totalRatings = totalRatings,
    favoriteSports = emptyList(),
    city = "Porto Alegre",
    neighborhood = "Menino Deus",
    memberSinceMs = 1_735_689_600_000L,
)

internal fun playerSearchResult(
    userId: String = "player-1",
    displayName: String = "Ana Souza",
    averageRating: Float = 4.5f,
    sports: List<Sport> = emptyList(),
): PlayerSearchResult = PlayerSearchResult(
    userId = userId,
    displayName = displayName,
    photoUrl = null,
    averageRating = averageRating,
    totalRatings = 12,
    favoriteSports = sports,
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
