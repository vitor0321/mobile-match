package com.walcker.identity.features.data.remote

import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.error.verificationErrorForIosCode
import platform.Foundation.NSError

private const val FIREBASE_AUTH_ERROR_DOMAIN = "FIRAuthErrorDomain"

internal fun NSError.toVerificationError(): VerificationError = if (domain == FIREBASE_AUTH_ERROR_DOMAIN) verificationErrorForIosCode(code) else VerificationError.Unknown
