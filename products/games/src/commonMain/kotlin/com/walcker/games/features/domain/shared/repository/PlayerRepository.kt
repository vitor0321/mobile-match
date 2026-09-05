package com.walcker.games.features.domain.shared.repository

import com.walcker.games.features.domain.shared.model.PlayerDetails
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.PlayerSearchResults
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage

internal interface PlayerRepository {
    suspend fun searchPlayers(filters: PlayerSearchFilters): Result<PlayerSearchResults>

    suspend fun getPlayerDetails(userId: String): Result<PlayerDetails>

    suspend fun getPlayersRatingSummary(userIds: List<String>): Result<Map<String, PlayerRatingSummary>>

    suspend fun getPlayerRatings(
        userId: String,
        limit: Int = 20,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>
}
