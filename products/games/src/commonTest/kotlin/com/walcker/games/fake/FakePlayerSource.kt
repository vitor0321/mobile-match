package com.walcker.games.fake

import com.walcker.games.features.data.shared.source.PlayerDetailsDto
import com.walcker.games.features.data.shared.source.PlayerSearchPageDto
import com.walcker.games.features.data.shared.source.PlayerSearchResultDto
import com.walcker.games.features.data.shared.source.PlayerSource
import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage

internal class FakePlayerSource(
    var searchResult: Result<PlayerSearchPageDto> =
        Result.success(
            PlayerSearchPageDto(players = listOf(playerSearchResultDto()), reachedLimit = false),
        ),
    var detailsResult: Result<PlayerDetailsDto> = Result.success(playerDetailsDto()),
) : PlayerSource {
    var searchCallCount: Int = 0
        private set
    var detailsCallCount: Int = 0
        private set

    override suspend fun searchPlayers(
        filters: PlayerSearchFilters,
        limit: Int,
    ): Result<PlayerSearchPageDto> {
        searchCallCount++
        return searchResult
    }

    override suspend fun getPlayerDetails(userId: String): Result<PlayerDetailsDto> {
        detailsCallCount++
        return detailsResult
    }

    override suspend fun getPlayerRatings(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> = Result.success(RatingsPage.Empty)
}

internal fun playerSearchResultDto(
    userId: String = "player-1",
    fullName: String = "Ana Souza",
    rating: Float = 4.5f,
): PlayerSearchResultDto =
    PlayerSearchResultDto(
        userId = userId,
        fullName = fullName,
        avatarUrl = null,
        rating = rating,
        ratingCount = 12,
        sports = listOf("FUTEBOL"),
    )

internal fun playerDetailsDto(
    userId: String = "player-1",
    fullName: String = "Ana Souza",
): PlayerDetailsDto =
    PlayerDetailsDto(
        userId = userId,
        fullName = fullName,
        avatarUrl = null,
        rating = 4.5f,
        ratingCount = 12,
        sports = listOf("FUTEBOL"),
        city = "Porto Alegre",
        neighborhood = "Menino Deus",
        createdAtMs = 1_735_689_600_000L,
    )
