package com.walcker.match.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume

internal actual fun createReverseGeocoder(): ReverseGeocoder = IosReverseGeocoder()

@OptIn(ExperimentalForeignApi::class)
private class IosReverseGeocoder : ReverseGeocoder {
    private val geocoder = CLGeocoder()

    override suspend fun reverseGeocode(lat: Double, lng: Double): GeocodedAddress? =
        suspendCancellableCoroutine { continuation ->
            val location = CLLocation(latitude = lat, longitude = lng)
            geocoder.reverseGeocodeLocation(location) { placemarks, _ ->
                val placemark = placemarks?.firstOrNull() as? CLPlacemark
                if (placemark == null) {
                    continuation.resume(null)
                    return@reverseGeocodeLocation
                }
                val street = listOfNotNull(
                    placemark.thoroughfare,
                    placemark.subThoroughfare,
                ).joinToString(" ")
                continuation.resume(
                    GeocodedAddress(
                        address = street,
                        neighborhood = placemark.subLocality ?: "",
                        city = placemark.locality ?: "",
                    ),
                )
            }
        }
}
