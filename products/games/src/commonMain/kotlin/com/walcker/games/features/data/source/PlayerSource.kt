package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.DimensionAverage
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.RatingDimension
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage

internal interface PlayerSource {
    suspend fun searchPlayers(
        filters: PlayerSearchFilters,
        limit: Int = DEFAULT_SEARCH_LIMIT,
    ): Result<PlayerSearchPageDto>

    suspend fun getPlayerDetails(userId: String): Result<PlayerDetailsDto>

    suspend fun getPlayerRatings(
        userId: String,
        limit: Int = 20,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>

    companion object {
        const val DEFAULT_SEARCH_LIMIT: Int = 50
    }
}

internal data class PlayerSearchPageDto(
    val players: List<PlayerSearchResultDto>,
    val reachedLimit: Boolean,
)

internal data class PlayerSearchResultDto(
    val userId: String,
    val fullName: String,
    val avatarUrl: String?,
    val rating: Float,
    val ratingCount: Int,
    val sports: List<String>,
)

internal data class PlayerDetailsDto(
    val userId: String,
    val fullName: String,
    val avatarUrl: String?,
    val rating: Float,
    val ratingCount: Int,
    val sports: List<String>,
    val city: String?,
    val neighborhood: String?,
    val createdAtMs: Long,
    val dimensionAverages: Map<RatingDimension, DimensionAverage> = emptyMap(),
)
