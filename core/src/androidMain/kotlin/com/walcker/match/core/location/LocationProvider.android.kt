package com.walcker.match.core.location

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.navigation.CurrentActivityHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

internal actual fun createLocationProvider(): LocationProvider =
    AndroidLocationProvider(
        application = KoinPlatform.getKoin().get<CurrentActivityHolder>().application
            ?: error("Application context not available; ensure CurrentActivityHolder is initialized"),
    )

private class AndroidLocationProvider(
    private val application: Application,
) : LocationProvider {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    override suspend fun requestPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return granted || requestPermissionViaActivity()
    }

    override suspend fun currentLocation(): Result<Coordinates> =
        runCatching {
            val location = getLastKnownLocation() ?: getFreshLocation()
            Coordinates(lat = location.latitude, lng = location.longitude)
        }.recoverCatching { e ->
            when (e) {
                is SecurityException -> throw LocationError.PermissionDenied
                else -> throw LocationError.Unavailable
            }
        }

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
        }

    @Suppress("MissingPermission")
    private suspend fun getFreshLocation(): android.location.Location {
        val result = withTimeoutOrNull(15.seconds) {
            suspendCancellableCoroutine { cont ->
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setMaxUpdates(1)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedClient.removeLocationUpdates(this)
                        val loc = result.lastLocation
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            cont.resumeWithException(LocationError.Timeout)
                        }
                    }
                }

                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

                cont.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                }
            }
        }
        return result ?: throw LocationError.Timeout
    }

    private suspend fun requestPermissionViaActivity(): Boolean {
        return LocationPermissionRequesterHolder.requester?.requestFineLocationPermission() ?: false
    }
}
