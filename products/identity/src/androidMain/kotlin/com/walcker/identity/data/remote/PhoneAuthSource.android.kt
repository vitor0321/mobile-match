package com.walcker.identity.features.data.remote

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession
import com.walcker.match.core.navigation.CurrentActivityHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

private const val CODE_TIMEOUT_SECONDS = 60L

internal class AndroidPhoneAuthSource(
    private val firebaseAuth: FirebaseAuth,
    private val activityHolder: CurrentActivityHolder,
) : PhoneAuthSource {
    override suspend fun sendCode(
        e164: String,
        resend: PhoneVerificationSession?,
        purpose: PhoneCredentialPurpose,
    ): Result<PhoneCodeResult> {
        val activity = activityHolder.currentActivity() ?: return Result.failure(VerificationError.NoForegroundActivity)
        val user = firebaseAuth.currentUser ?: return Result.failure(VerificationError.Unknown)
        firebaseAuth.useAppLanguage()

        return suspendCancellableCoroutine { continuation ->
            val callbacks =
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        if (!continuation.isActive) return
                        val session =
                            PhoneVerificationSession(
                                e164 = e164,
                                verificationId = verificationId,
                                platformResendToken = token,
                                purpose = purpose,
                            )
                        continuation.resume(Result.success(PhoneCodeResult.CodeSent(session)))
                    }

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        fun onApplied(updatedUser: FirebaseUser) {
                            updatedUser.getIdToken(true)
                            if (continuation.isActive) {
                                continuation.resume(Result.success(PhoneCodeResult.AutoVerified(updatedUser.toUserSession())))
                            }
                        }

                        fun onFailed(error: Exception) {
                            if (continuation.isActive) continuation.resume(Result.failure(error.toVerificationError()))
                        }

                        when (purpose) {
                            PhoneCredentialPurpose.Link ->
                                user
                                    .linkWithCredential(credential)
                                    .addOnSuccessListener { result -> onApplied(result.user ?: user) }
                                    .addOnFailureListener { error -> onFailed(error) }
                            PhoneCredentialPurpose.Replace ->
                                user
                                    .updatePhoneNumber(credential)
                                    .addOnSuccessListener { onApplied(user) }
                                    .addOnFailureListener { error -> onFailed(error) }
                        }
                    }

                    override fun onVerificationFailed(exception: FirebaseException) {
                        if (continuation.isActive) continuation.resume(Result.failure(exception.toVerificationError()))
                    }
                }

            val options =
                PhoneAuthOptions
                    .newBuilder(firebaseAuth)
                    .setPhoneNumber(e164)
                    .setTimeout(CODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
            (resend?.platformResendToken as? PhoneAuthProvider.ForceResendingToken)?.let { token ->
                options.setForceResendingToken(token)
            }
            PhoneAuthProvider.verifyPhoneNumber(options.build())
        }
    }

    override suspend fun confirmCode(
        session: PhoneVerificationSession,
        code: String,
    ): Result<UserSession> =
        runCatching {
            val user = firebaseAuth.currentUser ?: throw VerificationError.Unknown
            val credential = PhoneAuthProvider.getCredential(session.verificationId, code)
            user.applyPhoneCredential(credential, session.purpose).toUserSession()
        }.onFailure { error -> if (error is CancellationException) throw error }
            .recoverCatching { error -> throw error.toVerificationError() }
}

private suspend fun FirebaseUser.applyPhoneCredential(
    credential: PhoneAuthCredential,
    purpose: PhoneCredentialPurpose,
): FirebaseUser =
    when (purpose) {
        PhoneCredentialPurpose.Link -> linkWithCredential(credential).await().user ?: this
        PhoneCredentialPurpose.Replace -> {
            updatePhoneNumber(credential).await()
            this
        }
    }
