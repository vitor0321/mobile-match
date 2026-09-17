package com.walcker.identity.features.data.remote

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession

internal interface PhoneAuthSource {
    suspend fun sendCode(
        e164: String,
        resend: PhoneVerificationSession?,
        purpose: PhoneCredentialPurpose,
    ): Result<PhoneCodeResult>

    suspend fun confirmCode(
        session: PhoneVerificationSession,
        code: String,
    ): Result<UserSession>
}
