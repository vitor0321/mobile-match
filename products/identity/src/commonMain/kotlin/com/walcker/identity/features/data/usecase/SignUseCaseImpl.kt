package com.walcker.identity.features.data.usecase

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.repository.AuthRepository
import com.walcker.identity.features.domain.usecase.SignUseCase
import kotlinx.coroutines.flow.Flow

internal class SignUseCaseImpl(
    private val authRepository: AuthRepository,
) : SignUseCase {
    override fun observeSession(): Flow<UserSession?> = authRepository.currentUser

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<UserSession> = authRepository.signIn(email = email, password = password)

    override suspend fun signInWithGoogle(): Result<UserSession> = authRepository.signInWithGoogle()

    override suspend fun signInWithApple(): Result<UserSession> = authRepository.signInWithApple()

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserSession> = authRepository.signUp(email = email, password = password, displayName = displayName)

    override suspend fun deleteAccount(): Result<Unit> = authRepository.deleteAccount()

    override suspend fun signOut(): Result<Unit> = authRepository.signOut()

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = authRepository.sendPasswordResetEmail(email)
}
