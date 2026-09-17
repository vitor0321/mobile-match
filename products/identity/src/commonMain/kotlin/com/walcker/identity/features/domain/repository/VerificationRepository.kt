package com.walcker.identity.features.domain.repository

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession
import kotlinx.coroutines.flow.Flow

internal interface VerificationRepository {
    val currentUser: Flow<UserSession?>

    suspend fun refreshSession(): Result<UserSession>

    suspend fun sendEmailVerification(): Result<Unit>

    suspend fun sendPhoneCode(
        e164: String,
        resend: PhoneVerificationSession? = null,
        purpose: PhoneCredentialPurpose = PhoneCredentialPurpose.Link,
    ): Result<PhoneCodeResult>

    suspend fun confirmPhoneCode(
        session: PhoneVerificationSession,
        code: String,
    ): Result<UserSession>
}
