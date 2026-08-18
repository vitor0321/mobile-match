package com.walcker.games.features.domain.model

/**
 * Search result for a player in the player search feature.
 *
 * Contains essential player information for displaying in search results.
 * For detailed info, fetch [PlayerDetails] separately.
 */
internal data class PlayerSearchResult(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val averageRating: Float,
    val totalRatings: Int,
    val favoriteSports: List<Sport>,
    val matchesOrganized: Int,
    val matchesParticipated: Int,
)