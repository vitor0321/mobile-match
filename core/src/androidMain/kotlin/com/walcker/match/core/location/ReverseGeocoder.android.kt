package com.walcker.match.core.location

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.walcker.match.core.navigation.CurrentActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.util.Locale
import kotlin.coroutines.resume

internal actual fun createReverseGeocoder(): ReverseGeocoder =
    AndroidReverseGeocoder(
        application =
            KoinPlatform.getKoin().get<CurrentActivityHolder>().application
                ?: error("Application context not available; ensure CurrentActivityHolder is initialized"),
    )

private class AndroidReverseGeocoder(
    application: Application,
) : ReverseGeocoder {
    private val geocoder = Geocoder(application, Locale.getDefault())

    override suspend fun reverseGeocode(
        lat: Double,
        lng: Double,
    ): GeocodedAddress? {
        val address = fetchAddress(lat, lng) ?: return null
        val street =
            listOfNotNull(
                address.thoroughfare,
                address.subThoroughfare,
            ).joinToString(" ").ifBlank { address.getAddressLine(0) ?: "" }

        return GeocodedAddress(
            address = street,
            neighborhood = address.subLocality ?: "",
            city = address.locality ?: address.subAdminArea ?: "",
        )
    }

    private suspend fun fetchAddress(
        lat: Double,
        lng: Double,
    ): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fetchAddressModern(lat, lng)
        } else {
            fetchAddressLegacy(lat, lng)
        }

    private suspend fun fetchAddressModern(
        lat: Double,
        lng: Double,
    ): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                continuation.resume(addresses.firstOrNull())
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun fetchAddressLegacy(
        lat: Double,
        lng: Double,
    ): Address? =
        withContext(Dispatchers.Default) {
            runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
        }
}
