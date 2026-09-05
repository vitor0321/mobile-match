package com.walcker.match.core.location

public data class GeocodedLocation(
    val lat: Double,
    val lng: Double,
    val address: String,
    val neighborhood: String,
    val city: String,
)

public interface AddressGeocoder {
    public suspend fun geocodeAddress(query: String): GeocodedLocation?
}

internal expect fun createAddressGeocoder(): AddressGeocoder
