package com.walcker.match.core.location

import com.walcker.match.core.geo.Coordinates

public interface LocationProvider {
    /**
     * Asks for location permission (shows the OS dialog on both platforms).
     * Returns `true` if the user granted or pre-granted the permission.
     */
    suspend fun requestPermission(): Boolean

    /**
     * Returns the current GPS position. A successful call implies permission
     * was already granted; callers should call [requestPermission] first.
     */
    suspend fun currentLocation(): Result<Coordinates>
}

internal expect fun createLocationProvider(): LocationProvider
