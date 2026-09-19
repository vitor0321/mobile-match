@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.walcker.identity.features.data.remote

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIRPhoneAuthProvider
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume

internal class IosPhoneAuthSource(
    private val auth: FIRAuth,
) : PhoneAuthSource {
    override suspend fun sendCode(
        e164: String,
        resend: PhoneVerificationSession?,
        purpose: PhoneCredentialPurpose,
    ): Result<PhoneCodeResult> {
        auth.useAppLanguage()
        return suspendCancellableCoroutine { continuation ->
            FIRPhoneAuthProvider.providerWithAuth(auth).verifyPhoneNumber(e164, null) { verificationId: String?, error: NSError? ->
                continuation.resume(
                    when {
                        error != null -> Result.failure(error.toVerificationError())
                        verificationId == null -> Result.failure(VerificationError.Unknown)
                        else ->
                            Result.success(
                                PhoneCodeResult.CodeSent(
                                    PhoneVerificationSession(
                                        e164 = e164,
                                        verificationId = verificationId,
                                        platformResendToken = null,
                                        purpose = purpose,
                                    ),
                                ),
                            )
                    },
                )
            }
        }
    }

    override suspend fun confirmCode(
        session: PhoneVerificationSession,
        code: String,
    ): Result<UserSession> {
        val user = auth.currentUser() ?: return Result.failure(VerificationError.Unknown)
        val credential = FIRPhoneAuthProvider.providerWithAuth(auth).credentialWithVerificationID(session.verificationId, code)
        return when (session.purpose) {
            PhoneCredentialPurpose.Link ->
                suspendCancellableCoroutine { continuation ->
                    user.linkWithCredential(credential) { result: FIRAuthDataResult?, error: NSError? ->
                        continuation.resume(
                            if (error != null) {
                                Result.failure(error.toVerificationError())
                            } else {
                                Result.success((result?.user() ?: user).toUserSession())
                            },
                        )
                    }
                }
            PhoneCredentialPurpose.Replace ->
                suspendCancellableCoroutine { continuation ->
                    user.updatePhoneNumberCredential(credential) { error: NSError? ->
                        continuation.resume(
                            if (error != null) Result.failure(error.toVerificationError()) else Result.success(user.toUserSession()),
                        )
                    }
                }
        }
    }
}
