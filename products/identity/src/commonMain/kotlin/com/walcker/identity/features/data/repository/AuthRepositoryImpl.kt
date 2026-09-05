package com.walcker.identity.features.data.repository

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.AppleAuthSource
import com.walcker.identity.features.data.remote.FirebaseAuthSource
import com.walcker.identity.features.data.remote.GoogleAuthSource
import com.walcker.identity.features.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

internal class AuthRepositoryImpl(
    private val firebaseAuthSource: FirebaseAuthSource,
    private val googleAuthSource: GoogleAuthSource,
    private val appleAuthSource: AppleAuthSource,
) : AuthRepository {
    override val currentUser: Flow<UserSession?> = firebaseAuthSource.currentUser

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<UserSession> = firebaseAuthSource.signIn(email = email, password = password)

    override suspend fun signInWithGoogle(): Result<UserSession> = googleAuthSource.signIn()

    override suspend fun signInWithApple(): Result<UserSession> = appleAuthSource.signIn()

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserSession> =
        firebaseAuthSource.signUp(email = email, password = password, displayName = displayName)

    override suspend fun deleteAccount(): Result<Unit> = firebaseAuthSource.deleteCurrentUser()

    override suspend fun signOut(): Result<Unit> = firebaseAuthSource.signOut()

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = firebaseAuthSource.sendPasswordResetEmail(email)
}
