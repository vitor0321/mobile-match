package com.walcker.match.core.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
private const val EARTH_MERIDIONAL_CIRCUMFERENCE = 40007.86
private const val METERS_PER_DEGREE_LATITUDE = 110574.0

public data class GeoHashRange(val start: String, val endInclusive: String)

public fun encodeGeoHash(coords: Coordinates, precision: Int = DEFAULT_GEOHASH_PRECISION): String {
    require(precision in 1..22) { "geohash precision must be in 1..22" }

    val hash = StringBuilder(precision)
    var latMin = -90.0
    var latMax = 90.0
    var lngMin = -180.0
    var lngMax = 180.0
    var even = true
    var bit = 0
    var ch = 0

    while (hash.length < precision) {
        if (even) {
            val mid = (lngMin + lngMax) / 2
            if (coords.lng >= mid) {
                ch = ch or BITS[bit]
                lngMin = mid
            } else {
                lngMax = mid
            }
        } else {
            val mid = (latMin + latMax) / 2
            if (coords.lat >= mid) {
                ch = ch or BITS[bit]
                latMin = mid
            } else {
                latMax = mid
            }
        }
        even = !even
        if (bit < 4) {
            bit++
        } else {
            hash.append(BASE32[ch])
            bit = 0
            ch = 0
        }
    }

    return hash.toString()
}

public fun boundsForRadius(
    center: Coordinates,
    radiusKm: Double,
): List<GeoHashRange> {
    require(radiusKm > 0) { "radius must be positive" }

    val precision = precisionForRadius(radiusKm)
    val queryBits = precision * BITS_PER_CHAR

    val latDegrees = radiusKm / (METERS_PER_DEGREE_LATITUDE / 1000.0)
    val lngDegrees = kmToLongitudeDegrees(radiusKm, center.lat)

    val latMin = center.lat - latDegrees
    val latMax = center.lat + latDegrees
    val lngMin = center.lng - lngDegrees
    val lngMax = center.lng + lngDegrees

    val corners = listOf(
        Coordinates(latMin, lngMin),
        Coordinates(latMin, lngMax),
        Coordinates(latMax, lngMin),
        Coordinates(latMax, lngMax),
        Coordinates(center.lat, lngMin),
        Coordinates(center.lat, lngMax),
        Coordinates(latMin, center.lng),
        Coordinates(latMax, center.lng),
    )

    val hashes = corners
        .map { encodeGeoHash(it, precision) }
        .toMutableSet()

    hashes.add(encodeGeoHash(center, precision))

    val ranges = hashes
        .sorted()
        .map { GeoHashRange(start = it, endInclusive = it + "~") }

    return mergeAdjacent(ranges)
}

private fun precisionForRadius(radiusKm: Double): Int {
    var precision = DEFAULT_GEOHASH_PRECISION
    while (precision > 1) {
        val cellKm = latitudeSpanKm(precision)
        if (cellKm > radiusKm * 2) return precision
        precision--
    }
    return 1
}

private fun latitudeSpanKm(precision: Int): Double {
    val latBits = (precision * BITS_PER_CHAR + 1) / 2
    val spanDegrees = 180.0 / (1 shl latBits)
    return (spanDegrees / 360.0) * EARTH_MERIDIONAL_CIRCUMFERENCE
}

private fun kmToLongitudeDegrees(radiusKm: Double, latitude: Double): Double {
    val radians = radiusKm / (METERS_PER_DEGREE_LATITUDE / 1000.0) * PI / 180.0
    val latRad = latitude * PI / 180.0
    val numerator = sin(radians)
    val denom = cos(latRad)
    if (denom == 0.0 || numerator > denom) return 360.0
    return asin(numerator / denom) * 180.0 / PI
}

private fun mergeAdjacent(ranges: List<GeoHashRange>): List<GeoHashRange> {
    if (ranges.size <= 1) return ranges
    val merged = mutableListOf<GeoHashRange>()
    var current = ranges.first()
    for (i in 1 until ranges.size) {
        val next = ranges[i]
        if (current.endInclusive >= next.start) {
            current = GeoHashRange(
                start = current.start,
                endInclusive = maxOf(current.endInclusive, next.endInclusive),
            )
        } else {
            merged.add(current)
            current = next
        }
    }
    merged.add(current)
    return merged
}

public const val DEFAULT_GEOHASH_PRECISION: Int = 9
private const val BITS_PER_CHAR = 5
private val BITS = intArrayOf(16, 8, 4, 2, 1)
