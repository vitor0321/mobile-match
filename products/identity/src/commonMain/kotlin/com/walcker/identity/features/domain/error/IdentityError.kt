package com.walcker.identity.features.domain.error

/**
 * Errors that may safely cross the Identity data boundary. SDK exceptions must
 * be mapped to one of these values before reaching presentation code.
 */
internal sealed class IdentityError : Exception() {
    data object Cancelled : IdentityError()
    data object InvalidCredentials : IdentityError()
    data object EmailAlreadyInUse : IdentityError()
    data object Network : IdentityError()
    data object ProviderUnavailable : IdentityError()
    data object Configuration : IdentityError()
    data object Unknown : IdentityError()
}
