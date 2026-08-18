package com.walcker.games.features.ui.map

import com.walcker.games.features.domain.model.Game
import com.walcker.match.core.geo.Coordinates
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * A match with calculated distance from user location.
 */
internal data class NearbyMatch(
    val game: Game,
    val distanceKm: Double,
)

/**
 * Calculate distance between two coordinates in kilometers using Haversine formula.
 */
internal fun calculateDistance(from: Coordinates, to: Coordinates): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(to.lat - from.lat)
    val dLng = Math.toRadians(to.lng - from.lng)
    val fromLatRad = Math.toRadians(from.lat)
    val toLatRad = Math.toRadians(to.lat)

    val a = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLng / 2) * sin(dLng / 2) * cos(fromLatRad) * cos(toLatRad)
    val c = 2 * acos(kotlin.math.sqrt(a))
    return earthRadiusKm * c
}
