package com.walcker.match.core.location

import com.walcker.match.core.geo.Coordinates
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

internal actual fun createLocationProvider(): LocationProvider = IosLocationProvider()

private class IosLocationProvider : LocationProvider {
    private val locationManager = CLLocationManager().apply {
        desiredAccuracy = kCLLocationAccuracyBest
    }

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { cont ->
        val status = locationManager.authorizationStatus
        if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        if (status == kCLAuthorizationStatusDenied) {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
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
    override suspend fun currentLocation(): Result<Coordinates> = suspendCancellableCoroutine { cont ->
        val status = locationManager.authorizationStatus
        if (status == kCLAuthorizationStatusDenied) {
            cont.resume(Result.failure(LocationError.PermissionDenied))
            return@suspendCancellableCoroutine
        }

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            @Suppress("CONFLICTING_OVERLOADS")
            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>,
            ) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                if (location != null) {
                    manager.stopUpdatingLocation()
                    manager.delegate = null
                    val coords = Coordinates(
                        lat = location.coordinate.useContents { latitude },
                        lng = location.coordinate.useContents { longitude },
                    )
                    cont.resume(Result.success(coords))
                }
            }

            @Suppress("CONFLICTING_OVERLOADS")
            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                manager.stopUpdatingLocation()
                manager.delegate = null
                cont.resume(Result.failure(LocationError.Unavailable))
            }
        }
        locationManager.delegate = delegate
        locationManager.startUpdatingLocation()
    }
}
