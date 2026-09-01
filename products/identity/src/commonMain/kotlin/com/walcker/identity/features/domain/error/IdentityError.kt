package com.walcker.identity.features.domain.error

internal sealed class IdentityError : Exception() {
    data object Cancelled : IdentityError()

    data object InvalidCredentials : IdentityError()

    data object EmailAlreadyInUse : IdentityError()

    data object Network : IdentityError()

    data object ProviderUnavailable : IdentityError()

    data object Configuration : IdentityError()

    data object Unknown : IdentityError()
}
