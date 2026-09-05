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

internal actual fun createAddressGeocoder(): AddressGeocoder =
    AndroidAddressGeocoder(
        application =
            KoinPlatform.getKoin().get<CurrentActivityHolder>().application
                ?: error("Application context not available; ensure CurrentActivityHolder is initialized"),
    )

private class AndroidAddressGeocoder(
    application: Application,
) : AddressGeocoder {
    private val geocoder = Geocoder(application, Locale.getDefault())

    override suspend fun geocodeAddress(query: String): GeocodedLocation? {
        val address = fetchAddress(query) ?: return null
        val street =
            listOfNotNull(
                address.thoroughfare,
                address.subThoroughfare,
            ).joinToString(" ").ifBlank { address.getAddressLine(0) ?: "" }

        return GeocodedLocation(
            lat = address.latitude,
            lng = address.longitude,
            address = street,
            neighborhood = address.subLocality ?: "",
            city = address.locality ?: address.subAdminArea ?: "",
        )
    }

    private suspend fun fetchAddress(query: String): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fetchAddressModern(query)
        } else {
            fetchAddressLegacy(query)
        }

    private suspend fun fetchAddressModern(query: String): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(query, 1) { addresses ->
                continuation.resume(addresses.firstOrNull())
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun fetchAddressLegacy(query: String): Address? =
        withContext(Dispatchers.Default) {
            runCatching { geocoder.getFromLocationName(query, 1)?.firstOrNull() }.getOrNull()
        }
}
