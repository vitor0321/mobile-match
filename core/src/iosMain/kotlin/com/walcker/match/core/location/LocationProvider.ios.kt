package com.walcker.match.core.location

import com.walcker.match.core.geo.Coordinates
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSinceDate
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

private const val MAX_CACHED_LOCATION_AGE_SECONDS = 300.0
private const val MAX_LOCATION_AGE_SECONDS = 30.0

internal actual fun createLocationProvider(): LocationProvider = IosLocationProvider()

private class IosLocationProvider : LocationProvider {
    private val locationManager =
        CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBest
        }

    override suspend fun requestPermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            val status = locationManager.authorizationStatus
            if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }
            if (status == kCLAuthorizationStatusDenied) {
                cont.resume(false)
                return@suspendCancellableCoroutine
            }

            val delegate =
                object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                        val newStatus = manager.authorizationStatus
                        if (newStatus != kCLAuthorizationStatusNotDetermined) {
                            manager.delegate = null
                            cont.resume(
                                newStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
                                    newStatus == kCLAuthorizationStatusAuthorizedAlways,
                            )
                        }
                    }
                }
            locationManager.delegate = delegate
            locationManager.requestWhenInUseAuthorization()
        }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun currentLocation(): Result<Coordinates> {
        val status = locationManager.authorizationStatus
        if (status == kCLAuthorizationStatusDenied) {
            return Result.failure(LocationError.PermissionDenied)
        }

        cachedLocationOrNull()?.let { return Result.success(it) }

        val result =
            withTimeoutOrNull(15.seconds) {
                suspendCancellableCoroutine { cont ->
                    val delegate =
                        object : NSObject(), CLLocationManagerDelegateProtocol {
                            @Suppress("CONFLICTING_OVERLOADS")
                            override fun locationManager(
                                manager: CLLocationManager,
                                didUpdateLocations: List<*>,
                            ) {
                                val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                                val ageSeconds = NSDate().timeIntervalSinceDate(location.timestamp)
                                if (ageSeconds > MAX_LOCATION_AGE_SECONDS) return

                                manager.stopUpdatingLocation()
                                manager.delegate = null
                                val coords =
                                    Coordinates(
                                        lat = location.coordinate.useContents { latitude },
                                        lng = location.coordinate.useContents { longitude },
                                    )
                                cont.resume(Result.success(coords))
                            }

                            @Suppress("CONFLICTING_OVERLOADS")
                            override fun locationManager(
                                manager: CLLocationManager,
                                didFailWithError: NSError,
                            ) {
                                manager.stopUpdatingLocation()
                                manager.delegate = null
                                cont.resume(Result.failure(LocationError.Unavailable))
                            }
                        }
                    cont.invokeOnCancellation {
                        locationManager.stopUpdatingLocation()
                        locationManager.delegate = null
                    }
                    locationManager.delegate = delegate
                    locationManager.startUpdatingLocation()
                }
            }
        return result ?: Result.failure(LocationError.Timeout)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cachedLocationOrNull(): Coordinates? {
        val location = locationManager.location ?: return null
        val ageSeconds = NSDate().timeIntervalSinceDate(location.timestamp)
        if (ageSeconds > MAX_CACHED_LOCATION_AGE_SECONDS) return null
        return Coordinates(
            lat = location.coordinate.useContents { latitude },
            lng = location.coordinate.useContents { longitude },
        )
    }
}
