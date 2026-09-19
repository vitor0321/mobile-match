package com.walcker.match.core.location

public interface LocationPermissionRequester {
    public suspend fun requestFineLocationPermission(): Boolean
}

public object LocationPermissionRequesterHolder {
    public var requester: LocationPermissionRequester? = null
}
