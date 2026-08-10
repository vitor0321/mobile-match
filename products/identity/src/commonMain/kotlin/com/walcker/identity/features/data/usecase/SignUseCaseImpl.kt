package com.walcker.identity.features.data.usecase

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.repository.AuthRepository
import com.walcker.identity.features.domain.usecase.SignUseCase
import kotlinx.coroutines.flow.Flow

internal class SignUseCaseImpl(
    private val authRepository: AuthRepository,
) : SignUseCase {
    override fun observeSession(): Flow<UserSession?> {
        return authRepository.currentUser
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> {
        return authRepository.signIn(email = email, password = password)
    }

    override suspend fun signInWithGoogle(): Result<UserSession> {
        return authRepository.signInWithGoogle()
    }

    override suspend fun signInWithApple(): Result<UserSession> {
        return authRepository.signInWithApple()
    }

    override suspend fun signUp(email: String, password: String): Result<UserSession> {
        return authRepository.signUp(email = email, password = password)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return authRepository.deleteAccount()
    }

    override suspend fun signOut(): Result<Unit> {
        return authRepository.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }
}

