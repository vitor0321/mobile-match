package com.walcker.match.core.location

public data class GeocodedAddress(
    val address: String,
    val neighborhood: String,
    val city: String,
)

public interface ReverseGeocoder {
    public suspend fun reverseGeocode(lat: Double, lng: Double): GeocodedAddress?
}

internal expect fun createReverseGeocoder(): ReverseGeocoder
