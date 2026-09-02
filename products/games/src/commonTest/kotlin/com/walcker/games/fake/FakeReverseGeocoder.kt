package com.walcker.games.fake

import com.walcker.match.core.location.GeocodedAddress
import com.walcker.match.core.location.ReverseGeocoder

internal class FakeReverseGeocoder(
    var result: GeocodedAddress? =
        GeocodedAddress(address = "Rua Um, 100", neighborhood = "Centro", city = "São Paulo"),
) : ReverseGeocoder {
    val calls: MutableList<Pair<Double, Double>> = mutableListOf()

    override suspend fun reverseGeocode(
        lat: Double,
        lng: Double,
    ): GeocodedAddress? {
        calls += lat to lng
        return result
    }
}
