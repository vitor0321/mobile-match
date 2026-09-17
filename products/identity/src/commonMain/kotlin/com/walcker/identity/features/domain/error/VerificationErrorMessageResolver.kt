package com.walcker.identity.features.domain.error

import com.walcker.identity.strings.VerificationStrings

internal fun Throwable.verificationMessage(strings: VerificationStrings): String =
    when (this) {
        VerificationError.TooManyRequests -> strings.errorTooManyRequests
        VerificationError.Network -> strings.errorNetwork
        VerificationError.InvalidPhoneNumber -> strings.errorInvalidPhoneNumber
        VerificationError.InvalidCode -> strings.errorInvalidCode
        VerificationError.CodeExpired -> strings.errorCodeExpired
        VerificationError.PhoneAlreadyInUse -> strings.errorPhoneAlreadyInUse
        VerificationError.RequiresRecentLogin -> strings.errorRequiresRecentLogin
        else -> strings.errorGeneric
    }
