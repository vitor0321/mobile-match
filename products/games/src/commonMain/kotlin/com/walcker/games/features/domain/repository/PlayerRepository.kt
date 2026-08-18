package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.model.Rating

/**
 * Repository for player-related data access.
 *
 * Handles fetching player search results, detailed profiles, and ratings.
 */
internal interface PlayerRepository {
    /**
     * Search for players by name and filters.
     *
     * @param filters Player search filters (name, rating, sports, matches, etc)
     * @return List of matching players, sorted per [filters.sortBy]
     */
    suspend fun searchPlayers(filters: PlayerSearchFilters): Result<List<PlayerSearchResult>>

    /**
     * Fetch detailed profile for a specific player.
     *
     * @param userId ID of the player
     * @return Complete player profile with stats
     */
    suspend fun getPlayerDetails(userId: String): Result<PlayerDetails>

    /**
     * Fetch ratings/reviews received by a player.
     *
     * Paginated with optional cursor for "load more".
     *
     * @param userId ID of the player being rated
     * @param limit Number of ratings to fetch (default 20)
     * @param cursor Optional pagination cursor (from previous call)
     * @return List of ratings in descending order by creation time
     */
    suspend fun getPlayerRatings(
        userId: String,
        limit: Int = 20,
        cursor: String? = null,
    ): Result<List<Rating>>
}
