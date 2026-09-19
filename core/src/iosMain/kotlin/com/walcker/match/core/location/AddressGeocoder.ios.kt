package com.walcker.match.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume

internal actual fun createAddressGeocoder(): AddressGeocoder = IosAddressGeocoder()

@OptIn(ExperimentalForeignApi::class)
private class IosAddressGeocoder : AddressGeocoder {
    private val geocoder = CLGeocoder()

    override suspend fun geocodeAddress(query: String): GeocodedLocation? =
        suspendCancellableCoroutine { continuation ->
            geocoder.geocodeAddressString(query) { placemarks, _ ->
                val placemark = placemarks?.firstOrNull() as? CLPlacemark
                val location = placemark?.location
                if (placemark == null || location == null) {
                    continuation.resume(null)
                    return@geocodeAddressString
                }
                val coordinate =
                    location.coordinate.useContents {
                        latitude to longitude
                    }
                val street =
                    listOfNotNull(
                        placemark.thoroughfare,
                        placemark.subThoroughfare,
                    ).joinToString(" ")
                continuation.resume(
                    GeocodedLocation(
                        lat = coordinate.first,
                        lng = coordinate.second,
                        address = street,
                        neighborhood = placemark.subLocality ?: "",
                        city = placemark.locality ?: "",
                    ),
                )
            }
        }
}
