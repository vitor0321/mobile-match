package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResults
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage

/**
 * Repository for player-related data access.
 *
 * Handles fetching player search results, detailed profiles, and ratings.
 */
internal interface PlayerRepository {
    /**
     * Search for players by name and filters.
     *
     * @param filters Player search filters (name, rating, sports)
     * @return Matching players sorted per [filters.sortBy], plus whether the
     *         read cap was reached
     */
    suspend fun searchPlayers(filters: PlayerSearchFilters): Result<PlayerSearchResults>

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
     * Paginated with an opaque cursor for "load more"; ordering is applied
     * server-side so paging never reshuffles what the user already scrolled past.
     *
     * @param userId ID of the player being rated
     * @param limit Number of ratings to fetch (default 20)
     * @param sort Ordering applied to the whole result set
     * @param cursor Optional pagination cursor (from the previous page)
     * @return A page of ratings plus the cursor for the next one
     */
    suspend fun getPlayerRatings(
        userId: String,
        limit: Int = 20,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>
}
