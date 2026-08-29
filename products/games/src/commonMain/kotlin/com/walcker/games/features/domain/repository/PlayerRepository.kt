package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResults
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage

internal interface PlayerRepository {
    suspend fun searchPlayers(filters: PlayerSearchFilters): Result<PlayerSearchResults>

    suspend fun getPlayerDetails(userId: String): Result<PlayerDetails>

    suspend fun getPlayerRatings(
        userId: String,
        limit: Int = 20,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>
}
