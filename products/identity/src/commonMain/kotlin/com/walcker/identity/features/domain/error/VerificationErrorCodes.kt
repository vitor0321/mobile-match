package com.walcker.identity.features.domain.error

internal fun verificationErrorForAndroidCode(errorCode: String?): VerificationError =
    when (errorCode) {
        "ERROR_TOO_MANY_REQUESTS", "ERROR_QUOTA_EXCEEDED" -> VerificationError.TooManyRequests
        "ERROR_INVALID_PHONE_NUMBER", "ERROR_MISSING_PHONE_NUMBER" -> VerificationError.InvalidPhoneNumber
        "ERROR_INVALID_VERIFICATION_CODE", "ERROR_MISSING_VERIFICATION_CODE" -> VerificationError.InvalidCode
        "ERROR_SESSION_EXPIRED", "ERROR_INVALID_VERIFICATION_ID" -> VerificationError.CodeExpired
        "ERROR_CREDENTIAL_ALREADY_IN_USE" -> VerificationError.PhoneAlreadyInUse
        "ERROR_PROVIDER_ALREADY_LINKED" -> VerificationError.AlreadyLinked
        "ERROR_REQUIRES_RECENT_LOGIN" -> VerificationError.RequiresRecentLogin
        else -> VerificationError.Unknown
    }

internal fun verificationErrorForIosCode(code: Long): VerificationError =
    when (code) {
        17010L, 17052L -> VerificationError.TooManyRequests
        17020L -> VerificationError.Network
        17041L, 17042L -> VerificationError.InvalidPhoneNumber
        17043L, 17044L -> VerificationError.InvalidCode
        17046L, 17051L -> VerificationError.CodeExpired
        17025L -> VerificationError.PhoneAlreadyInUse
        17015L -> VerificationError.AlreadyLinked
        17014L -> VerificationError.RequiresRecentLogin
        else -> VerificationError.Unknown
    }
