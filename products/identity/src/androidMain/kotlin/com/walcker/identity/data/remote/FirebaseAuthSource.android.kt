package com.walcker.identity.features.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.usecase.RequiresRecentLoginException
import com.walcker.identity.features.data.remote.FirebaseAuthSource as FeatureFirebaseAuthSource
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

private const val TAG = "FirebaseAuthSource"

internal actual fun createFirebaseAuthSource(stringsHolder: IdentityStringsHolder): FeatureFirebaseAuthSource {
    return AndroidFirebaseAuthSource(
        firebaseAuth = FirebaseAuth.getInstance(),
        stringsHolder = stringsHolder,
    )
}

internal class AndroidFirebaseAuthSource(
    private val firebaseAuth: FirebaseAuth,
    private val stringsHolder: IdentityStringsHolder,
) : FeatureFirebaseAuthSource {
    override val currentUser: Flow<UserSession?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUserSession())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toUserSession())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<UserSession> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        return suspendCancellableCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user?.toUserSession()
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        val errorMsg = strings.missingAuthenticatedUser
                        continuation.resume(Result.failure(IllegalStateException(errorMsg)))
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resume(Result.failure(error))
                }
                .addOnCanceledListener {
                    continuation.cancel()
                }
        }
    }

    override suspend fun signUp(email: String, password: String): Result<UserSession> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        return suspendCancellableCoroutine { continuation ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user?.toUserSession()
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        val errorMsg = strings.missingAuthenticatedUser
                        continuation.resume(Result.failure(IllegalStateException(errorMsg)))
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resume(Result.failure(error))
                }
                .addOnCanceledListener {
                    continuation.cancel()
                }
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            firebaseAuth.signOut()
        }.onFailure { error ->
            Log.e(TAG, "Erro no signOut: ${error.message}", error)
        }
    }

    override suspend fun deleteCurrentUser(): Result<Unit> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        return runCatching {
            val user = requireNotNull(firebaseAuth.currentUser) { strings.missingAuthenticatedUser }
            user.delete().await()
            Unit
        }.recoverCatching { error ->
            if (error is FirebaseAuthRecentLoginRequiredException) {
                throw RequiresRecentLoginException(error)
            }
            throw error
        }.onFailure { error ->
            Log.e(TAG, "Erro ao deletar usuário: ${error.message}", error)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Erro ao enviar email de redefinição: ${error.message}", error)
                    continuation.resume(Result.failure(error))
                }
                .addOnCanceledListener {
                    Log.w(TAG, "Envio de email de redefinição cancelado")
                    continuation.cancel()
                }
        }
    }
}

private fun FirebaseUser.toUserSession(): UserSession {
    return UserSession(
        uid = uid,
        email = email,
        displayName = displayName,
        creationTimestamp = metadata?.creationTimestamp,
    )
}

