package com.walcker.match.core.location

import com.walcker.match.core.geo.Coordinates

public interface LocationProvider {
    suspend fun requestPermission(): Boolean

    suspend fun currentLocation(): Result<Coordinates>
}

internal expect fun createLocationProvider(): LocationProvider
