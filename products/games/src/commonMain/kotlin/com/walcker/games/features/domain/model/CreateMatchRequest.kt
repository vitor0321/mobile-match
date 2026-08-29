package com.walcker.games.features.domain.model

internal data class CreateMatchRequest(
    val sport: Sport,
    val venueName: String,
    val neighborhood: String,
    val city: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val geohash: String,
    val startsAtSeconds: Long,
    val durationMin: Int,
    val totalPlayers: Int,
    val pricePerPlayer: String?,
)
