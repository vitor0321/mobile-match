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
    val joinRate: Float, // percentage: 0-100
    val cancelRate: Float, // percentage: 0-100
    val memberSince: Long, // unix epoch seconds
)
