package com.walcker.identity.fake

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.PhoneAuthSource
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession

internal class FakePhoneAuthSource(
    var sendCodeResult: Result<PhoneCodeResult> =
        Result.success(
            PhoneCodeResult.CodeSent(
                PhoneVerificationSession(e164 = "+5511912345678", verificationId = "verification-1", platformResendToken = null),
            ),
        ),
    var confirmCodeResult: Result<UserSession> = Result.failure(IllegalStateException("confirmCode not configured")),
) : PhoneAuthSource {
    val sendCodeCalls: MutableList<Pair<String, PhoneVerificationSession?>> = mutableListOf()
    val sendCodePurposes: MutableList<PhoneCredentialPurpose> = mutableListOf()
    val confirmCodeCalls: MutableList<Pair<PhoneVerificationSession, String>> = mutableListOf()

    override suspend fun sendCode(
        e164: String,
        resend: PhoneVerificationSession?,
        purpose: PhoneCredentialPurpose,
    ): Result<PhoneCodeResult> {
        sendCodeCalls += e164 to resend
        sendCodePurposes += purpose
        return sendCodeResult
    }

    override suspend fun confirmCode(
        session: PhoneVerificationSession,
        code: String,
    ): Result<UserSession> {
        confirmCodeCalls += session to code
        return confirmCodeResult
    }
}
