package com.walcker.identity.features.data.remote

import com.walcker.identity.api.UserSession
import com.walcker.identity.strings.IdentityStringsHolder
import kotlinx.coroutines.flow.Flow

internal interface FirebaseAuthSource {
    val currentUser: Flow<UserSession?>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<UserSession>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserSession>

    suspend fun deleteCurrentUser(): Result<Unit>

    suspend fun reauthenticateWithPassword(password: String): Result<Unit>

    suspend fun signInProvider(): Result<String?>

    suspend fun signOut(): Result<Unit>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun refreshSession(): Result<UserSession>

    suspend fun sendEmailVerification(): Result<Unit>
}

internal expect fun createFirebaseAuthSource(stringsHolder: IdentityStringsHolder): FirebaseAuthSource
