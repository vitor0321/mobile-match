package com.walcker.identity.features.domain.phone

import com.walcker.identity.api.UserSession

internal enum class PhoneCredentialPurpose { Link, Replace }

internal class PhoneVerificationSession(
    val e164: String,
    val verificationId: String,
    val platformResendToken: Any?,
    val purpose: PhoneCredentialPurpose = PhoneCredentialPurpose.Link,
)

internal sealed interface PhoneCodeResult {
    data class CodeSent(
        val session: PhoneVerificationSession,
    ) : PhoneCodeResult

    data class AutoVerified(
        val session: UserSession,
    ) : PhoneCodeResult
}
