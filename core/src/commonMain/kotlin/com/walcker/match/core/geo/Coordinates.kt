package com.walcker.match.core.geo

/**
 * A geographic point in WGS84.
 *
 * Latitude is bounded to [-90, 90] and longitude to [-180, 180], but this
 * class does not enforce it — validation lives at the boundaries that produce
 * coordinates (location source, Firestore mapper).
 */
public data class Coordinates(
    val lat: Double,
    val lng: Double,
)

/** The MVP defaults to São Paulo when the user has not shared a location yet. */
public val DefaultCenter: Coordinates = Coordinates(lat = -23.5505, lng = -46.6333)
