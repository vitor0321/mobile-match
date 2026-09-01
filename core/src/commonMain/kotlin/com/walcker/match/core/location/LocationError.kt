package com.walcker.match.core.location

public sealed class LocationError : Exception() {
    public data object PermissionDenied : LocationError()

    public data object Unavailable : LocationError()

    public data object Timeout : LocationError()
}
