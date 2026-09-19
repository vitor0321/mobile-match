package com.walcker.match.core.geo

public data class Coordinates(
    val lat: Double,
    val lng: Double,
)

public val DefaultCenter: Coordinates = Coordinates(lat = -23.5505, lng = -46.6333)
