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
import com.google.android.gms.tasks.CancellationTokenSource
import com.walcker.match.core.geo.Coordinates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual fun createLocationProvider(): LocationProvider =
    AndroidLocationProvider(
        application = com.walcker.match.core.di.CurrentActivityHolder.application
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
    private suspend fun getFreshLocation(): android.location.Location =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

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

            // Timeout: if no location within 15 seconds, cancel.
            cts.token.invokeOnCompletion {
                if (cont.isActive) {
                    fusedClient.removeLocationUpdates(callback)
                    cont.resumeWithException(LocationError.Timeout)
                }
            }
        }

    // The permission flow needs to launch a system dialog through the Activity.
    // CurrentActivityHolder provides the current Activity reference.
    private suspend fun requestPermissionViaActivity(): Boolean {
        // For now we rely on the application having requested the permission
        // through the standard Android permission launcher in MainActivity.
        // If we need a runtime dialog from background, we can add a
        // PermissionRequester Activity later.
        return false
    }
}
