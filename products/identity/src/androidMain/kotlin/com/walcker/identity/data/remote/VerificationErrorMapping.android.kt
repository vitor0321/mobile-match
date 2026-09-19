package com.walcker.identity.features.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.error.verificationErrorForAndroidCode

internal fun Throwable.toVerificationError(): VerificationError =
    when (this) {
        is VerificationError -> this
        is FirebaseNetworkException -> VerificationError.Network
        is FirebaseTooManyRequestsException -> VerificationError.TooManyRequests
        is FirebaseAuthException -> verificationErrorForAndroidCode(errorCode)
        else -> VerificationError.Unknown
    }
