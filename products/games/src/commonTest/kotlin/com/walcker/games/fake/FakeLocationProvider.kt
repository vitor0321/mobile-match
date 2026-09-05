package com.walcker.games.fake

import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.location.LocationProvider

internal class FakeLocationProvider(
    var permissionGranted: Boolean = true,
    var locationResult: Result<Coordinates> = Result.success(Coordinates(lat = -23.55, lng = -46.63)),
) : LocationProvider {
    override suspend fun requestPermission(): Boolean = permissionGranted

    override suspend fun currentLocation(): Result<Coordinates> = locationResult
}
