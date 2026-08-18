package com.walcker.games.features.domain.model

/**
 * Detailed player profile information.
 *
 * Fetched when user taps on a player from search results.
 * Contains all stats, preferences, and historical data.
 */
internal data class PlayerDetails(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val email: String,
    val bio: String?,
    val averageRating: Float,
    val totalRatings: Int,
    val matchesOrganized: Int,
    val matchesParticipated: Int,
    val favoriteSports: List<Sport>,
    val city: String?,
    val neighborhood: String?,
    val locationRadius: Int, // km
    val joinRate: Float, // fraction 0f..1f — format with formatPercent at the edge
    val cancelRate: Float, // fraction 0f..1f
    val memberSince: Long, // unix epoch seconds
)
