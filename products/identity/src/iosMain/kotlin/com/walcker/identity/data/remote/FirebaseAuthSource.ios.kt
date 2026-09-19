package com.walcker.identity.features.data.remote

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthCredential
import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIREmailAuthProvider
import cocoapods.FirebaseAuth.FIRUser
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.usecase.RequiresRecentLoginException
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import kotlin.coroutines.resume
import com.walcker.identity.features.data.remote.FirebaseAuthSource as FeatureFirebaseAuthSource

@OptIn(ExperimentalForeignApi::class)
internal actual fun createFirebaseAuthSource(stringsHolder: IdentityStringsHolder): FeatureFirebaseAuthSource =
    IosFirebaseAuthSource(
        auth = FIRAuth.auth(),
        stringsHolder = stringsHolder,
    )

@OptIn(ExperimentalForeignApi::class)
internal class IosFirebaseAuthSource(
    private val auth: FIRAuth,
    private val stringsHolder: IdentityStringsHolder,
) : FeatureFirebaseAuthSource {
    override val currentUser: Flow<UserSession?> =
        callbackFlow {
            val handle =
                auth.addIDTokenDidChangeListener { _, user ->
                    trySend(user?.toUserSession())
                }
            trySend(auth.currentUser()?.toUserSession())
            awaitClose { auth.removeIDTokenDidChangeListener(handle) }
        }.distinctUntilChanged()

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<UserSession> =
        suspendCancellableCoroutine { continuation ->
            auth.signInWithEmail(email = email, password = password) { result: FIRAuthDataResult?, error: NSError? ->
                continuation.resume(result.toResult(error, stringsHolder))
            }
        }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserSession> {
        val creationResult =
            suspendCancellableCoroutine { continuation ->
                auth.createUserWithEmail(email, password) { result, error ->
                    continuation.resume(result.toResult(error, stringsHolder))
                }
            }
        val session = creationResult.getOrElse { return Result.failure(it) }
        if (displayName.isBlank()) return Result.success(session)
        val user = auth.currentUser() ?: return Result.success(session)
        return suspendCancellableCoroutine { continuation ->
            val changeRequest = user.profileChangeRequest()
            changeRequest.setDisplayName(displayName)
            changeRequest.commitChangesWithCompletion {
                continuation.resume(Result.success(session.copy(displayName = displayName)))
            }
        }
    }

    override suspend fun signOut(): Result<Unit> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        return memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (auth.signOut(error.ptr)) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(strings.signOutFailed))
            }
        }
    }

    override suspend fun deleteCurrentUser(): Result<Unit> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        val user =
            auth.currentUser()
                ?: return Result.failure(IllegalStateException(strings.missingAuthenticatedUser))
        return suspendCancellableCoroutine { continuation ->
            user.deleteWithCompletion { error ->
                continuation.resume(
                    if (error == null) {
                        Result.success(Unit)
                    } else {
                        Result.failure(error.toDeleteAccountThrowable(strings.authErrorFallback))
                    },
                )
            }
        }
    }

    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        val user = auth.currentUser() ?: return Result.failure(IdentityError.Unknown)
        val email = user.email() ?: return Result.failure(IdentityError.Unknown)
        val credential = FIREmailAuthProvider.credentialWithEmail(email = email, password = password)
        return user.reauthenticate(credential, strings.authErrorFallback)
    }

    override suspend fun signInProvider(): Result<String?> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        val user = auth.currentUser() ?: return Result.failure(IdentityError.Unknown)
        return suspendCancellableCoroutine { continuation ->
            user.getIDTokenResultWithCompletion { result, error ->
                continuation.resume(
                    if (error != null) Result.failure(error.toThrowable(strings.authErrorFallback)) else Result.success(result?.signInProvider()),
                )
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        return suspendCancellableCoroutine { continuation ->
            auth.sendPasswordResetWithEmail(email = email) { error ->
                continuation.resume(
                    if (error == null) {
                        Result.success(Unit)
                    } else {
                        Result.failure(error.toThrowable(strings.authErrorFallback))
                    },
                )
            }
        }
    }

    override suspend fun refreshSession(): Result<UserSession> {
        val user = auth.currentUser() ?: return Result.failure(VerificationError.Unknown)
        val reloadError =
            suspendCancellableCoroutine<NSError?> { continuation ->
                user.reloadWithCompletion { error -> continuation.resume(error) }
            }
        if (reloadError != null) return Result.failure(reloadError.toVerificationError())
        val refreshed = auth.currentUser() ?: return Result.failure(VerificationError.Unknown)
        val tokenError =
            suspendCancellableCoroutine<NSError?> { continuation ->
                refreshed.getIDTokenForcingRefresh(true) { _, error -> continuation.resume(error) }
            }
        if (tokenError != null) return Result.failure(tokenError.toVerificationError())
        return Result.success(refreshed.toUserSession())
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        val user = auth.currentUser() ?: return Result.failure(VerificationError.Unknown)
        auth.useAppLanguage()
        return suspendCancellableCoroutine { continuation ->
            user.sendEmailVerificationWithCompletion { error ->
                continuation.resume(
                    if (error == null) Result.success(Unit) else Result.failure(error.toVerificationError()),
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun FIRAuthDataResult?.toResult(
    error: NSError?,
    stringsHolder: IdentityStringsHolder,
): Result<UserSession> {
    val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
    if (error != null) return Result.failure(error.toThrowable(strings.authErrorFallback))
    val user = this?.user()?.toUserSession()
    return if (user != null) {
        Result.success(user)
    } else {
        Result.failure(IllegalStateException(strings.missingAuthenticatedUser))
    }
}

internal fun NSError?.toThrowable(fallbackMessage: String): Throwable = IllegalStateException(this?.localizedDescription ?: fallbackMessage)

@OptIn(ExperimentalForeignApi::class)
internal suspend fun FIRUser.reauthenticate(
    credential: FIRAuthCredential,
    fallbackMessage: String,
): Result<Unit> {
    val reauthenticationError =
        suspendCancellableCoroutine<NSError?> { continuation ->
            reauthenticateWithCredential(credential) { _, error -> continuation.resume(error) }
        }
    if (reauthenticationError != null) return Result.failure(reauthenticationError.toReauthenticationError(fallbackMessage))
    val tokenError =
        suspendCancellableCoroutine<NSError?> { continuation ->
            getIDTokenForcingRefresh(true) { _, error -> continuation.resume(error) }
        }
    return if (tokenError != null) Result.failure(tokenError.toThrowable(fallbackMessage)) else Result.success(Unit)
}

private fun NSError.toReauthenticationError(fallbackMessage: String): Throwable =
    when (code) {
        17004L, 17009L -> IdentityError.InvalidCredentials
        17020L -> IdentityError.Network
        else -> toThrowable(fallbackMessage)
    }

private fun NSError.toDeleteAccountThrowable(fallbackMessage: String): Throwable =
    if (code == 17014L) {
        RequiresRecentLoginException(this.toThrowable(fallbackMessage))
    } else {
        toThrowable(fallbackMessage)
    }

@OptIn(ExperimentalForeignApi::class)
internal fun FIRUser.toUserSession(): UserSession =
    UserSession(
        uid = uid(),
        email = email(),
        displayName = displayName(),
        creationTimestamp = metadata()?.creationDate()?.timeIntervalSince1970()?.let { (it * 1000).toLong() },
        isEmailVerified = emailVerified(),
        phoneNumber = phoneNumber()?.takeIf { it.isNotBlank() },
    )
