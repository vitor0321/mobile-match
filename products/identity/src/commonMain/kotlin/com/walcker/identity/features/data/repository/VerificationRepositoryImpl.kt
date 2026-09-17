package com.walcker.identity.features.data.repository

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.FirebaseAuthSource
import com.walcker.identity.features.data.remote.PhoneAuthSource
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession
import com.walcker.identity.features.domain.repository.VerificationRepository
import kotlinx.coroutines.flow.Flow

internal class VerificationRepositoryImpl(
    private val firebaseAuthSource: FirebaseAuthSource,
    private val phoneAuthSource: PhoneAuthSource,
) : VerificationRepository {
    override val currentUser: Flow<UserSession?> = firebaseAuthSource.currentUser

    override suspend fun refreshSession(): Result<UserSession> = firebaseAuthSource.refreshSession()

    override suspend fun sendEmailVerification(): Result<Unit> = firebaseAuthSource.sendEmailVerification()

    override suspend fun sendPhoneCode(
        e164: String,
        resend: PhoneVerificationSession?,
        purpose: PhoneCredentialPurpose,
    ): Result<PhoneCodeResult> {
        val outcome =
            phoneAuthSource.sendCode(e164, resend, purpose).getOrElse { error ->
                return if (error.isAlreadyLinkedWhile(purpose)) refreshedAutoVerified() else Result.failure(error)
            }
        return when (outcome) {
            is PhoneCodeResult.CodeSent -> Result.success(outcome)
            is PhoneCodeResult.AutoVerified -> refreshedAutoVerified()
        }
    }

    override suspend fun confirmPhoneCode(
        session: PhoneVerificationSession,
        code: String,
    ): Result<UserSession> {
        phoneAuthSource.confirmCode(session, code).onFailure { error ->
            if (!error.isAlreadyLinkedWhile(session.purpose)) return Result.failure(error)
        }
        return firebaseAuthSource.refreshSession()
    }

    private suspend fun refreshedAutoVerified(): Result<PhoneCodeResult> = firebaseAuthSource.refreshSession().map { session -> PhoneCodeResult.AutoVerified(session) }

    private fun Throwable.isAlreadyLinkedWhile(purpose: PhoneCredentialPurpose): Boolean = this == VerificationError.AlreadyLinked && purpose == PhoneCredentialPurpose.Link
}
