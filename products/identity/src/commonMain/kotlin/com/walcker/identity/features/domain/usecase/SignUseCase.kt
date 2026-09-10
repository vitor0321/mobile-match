package com.walcker.identity.features.domain.usecase

import com.walcker.identity.api.UserSession
import kotlinx.coroutines.flow.Flow

internal interface SignUseCase {
    fun observeSession(): Flow<UserSession?>

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<UserSession>

    suspend fun signInWithGoogle(): Result<UserSession>

    suspend fun signInWithApple(): Result<UserSession>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserSession>

    suspend fun deleteAccount(): Result<Unit>

    suspend fun signOut(): Result<Unit>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
