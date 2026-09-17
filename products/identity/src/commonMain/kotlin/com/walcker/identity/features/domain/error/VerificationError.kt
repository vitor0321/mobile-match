package com.walcker.identity.features.domain.error

internal sealed class VerificationError : Exception() {
    data object TooManyRequests : VerificationError()

    data object Network : VerificationError()

    data object InvalidPhoneNumber : VerificationError()

    data object InvalidCode : VerificationError()

    data object CodeExpired : VerificationError()

    data object PhoneAlreadyInUse : VerificationError()

    data object AlreadyLinked : VerificationError()

    data object NoForegroundActivity : VerificationError()

    data object RequiresRecentLogin : VerificationError()

    data object Unknown : VerificationError()
}
