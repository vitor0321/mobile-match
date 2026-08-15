package com.walcker.games.features.domain.model

/**
 * Request to create a new match.
 *
 * Maps to Firestore `matches/{matchId}` document:
 * - Timestamps are stored as Firestore Timestamp (server-side)
 * - All fields are required except pricePerPlayer (null for free matches)
 */
internal data class CreateMatchRequest(
    val sport: Sport,
    val venueName: String,
    val neighborhood: String,
    val city: String,
    val address: String,
    // Location for geohash queries
    val lat: Double,
    val lng: Double,
    val geohash: String,
    // Start time: Unix seconds (will be stored as Firestore Timestamp)
    val startsAtSeconds: Long,
    // Duration in minutes
    val durationMin: Int,
    // Total players for this match
    val totalPlayers: Int,
    // Price per player, nullable (free matches have null)
    val pricePerPlayer: String?,
)
