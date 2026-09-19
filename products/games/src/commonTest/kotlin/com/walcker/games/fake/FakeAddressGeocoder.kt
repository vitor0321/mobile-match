package com.walcker.games.fake

import com.walcker.match.core.location.AddressGeocoder
import com.walcker.match.core.location.GeocodedLocation

internal class FakeAddressGeocoder(
    var result: GeocodedLocation? =
        GeocodedLocation(lat = -23.55, lng = -46.63, address = "Rua Um, 100", neighborhood = "Centro", city = "São Paulo"),
) : AddressGeocoder {
    val queries: MutableList<String> = mutableListOf()

    override suspend fun geocodeAddress(query: String): GeocodedLocation? {
        queries += query
        return result
    }
}
