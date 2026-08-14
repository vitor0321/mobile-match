package com.walcker.match.core.geo

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0

/**
 * Great-circle distance between two coordinates using the haversine formula.
 *
 * Mirrors the Postgres `distance_km` and the `haversine` in `broadcast.functions.ts`
 * from the Lovable MVP — same radius so client, server and legacy DB agree.
 */
public fun distanceKm(a: Coordinates, b: Coordinates): Double {
    val dLat = (b.lat - a.lat).toRadians()
    val dLng = (b.lng - a.lng).toRadians()
    val lat1 = a.lat.toRadians()
    val lat2 = b.lat.toRadians()

    val h = sin(dLat / 2).pow2() + cos(lat1) * cos(lat2) * sin(dLng / 2).pow2()
    val c = 2 * atan2(sqrt(h), sqrt(1 - h))
    return EARTH_RADIUS_KM * c
}

/**
 * Human-friendly distance for the pt-BR UI. Under 1 km rounds to the nearest
 * 10 m; above that shows one decimal ("3,2 km"). Matches `lib/geo.ts` from the MVP.
 */
public fun formatDistance(km: Double): String {
    if (km < 1.0) {
        val meters = (km * 1000).roundToInt()
        // Round meters to the nearest 10 to match the MVP's coarser display.
        val rounded = (meters.toDouble() / 10).roundToInt() * 10
        return "$rounded m"
    }
    val oneDecimal = ((km * 10).roundToInt() / 10.0)
    val whole = oneDecimal.toInt()
    val fraction = ((oneDecimal - whole) * 10).roundToInt()
    return "$whole,$fraction km"
}

private fun Double.toRadians(): Double = this * PI / 180.0
private fun Double.pow2(): Double = this * this
