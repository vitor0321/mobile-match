package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage

/**
 * Data source interface for player-related remote operations.
 *
 * Implemented by [FirestorePlayerSource] for Android and iOS.
 */
internal interface PlayerSource {
    /**
     * Search for players matching the given filters.
     *
     * @param filters Search and filter criteria (name, rating, sports, etc)
     * @return Raw DTOs from Firestore to be mapped by repository
     */
    suspend fun searchPlayers(filters: PlayerSearchFilters): Result<List<PlayerSearchResultDto>>

    /**
     * Fetch detailed player profile from Firestore.
     *
     * @param userId Player ID
     * @return Raw DTO to be mapped by repository
     */
    suspend fun getPlayerDetails(userId: String): Result<PlayerDetailsDto>

    /**
     * Fetch a page of ratings received by a player.
     *
     * @param userId Player being rated
     * @param limit Page size
     * @param sort Server-side ordering
     * @param cursor Opaque cursor from the previous page; `null` for the first
     * @return A page of ratings plus the cursor for the next one
     */
    suspend fun getPlayerRatings(
        userId: String,
        limit: Int = 20,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>
}

/**
 * DTO for player search result from Firestore.
 *
 * Maps to domain [com.walcker.games.features.domain.model.PlayerSearchResult].
 */
internal data class PlayerSearchResultDto(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val rating: Float,
    val ratingCount: Int,
    val sports: List<String>, // Sport enum names
    val matchesOrganized: Int,
    val matchesParticipated: Int,
)

/**
 * DTO for detailed player profile from Firestore.
 *
 * Maps to domain [com.walcker.games.features.domain.model.PlayerDetails].
 */
internal data class PlayerDetailsDto(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val email: String,
    val bio: String?,
    val rating: Float,
    val ratingCount: Int,
    val matchesOrganized: Int,
    val matchesParticipated: Int,
    val sports: List<String>, // Sport enum names
    val city: String?,
    val neighborhood: String?,
    val radiusKm: Int,
    val joinRate: Float,
    val cancelRate: Float,
    val memberSinceSeconds: Long,
)
