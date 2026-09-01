package com.walcker.identity.fake

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeAuthRepository(
    initialUser: UserSession? = null,
    private var signInResult: Result<UserSession> = Result.failure(IllegalStateException("signIn not configured")),
    private var signInWithGoogleResult: Result<UserSession> = Result.failure(IllegalStateException("signInWithGoogle not configured")),
    private var signInWithAppleResult: Result<UserSession> = Result.failure(IllegalStateException("signInWithApple not configured")),
    private var signUpResult: Result<UserSession> = Result.failure(IllegalStateException("signUp not configured")),
    private var deleteAccountResult: Result<Unit> = Result.success(Unit),
    private var signOutResult: Result<Unit> = Result.success(Unit),
    private var sendPasswordResetEmailResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {
    private val currentUserState = MutableStateFlow(initialUser)

    override val currentUser: Flow<UserSession?> = currentUserState.asStateFlow()

    var lastSignInInput: Pair<String, String>? = null
    var signInWithGoogleCallCount: Int = 0
    var signInWithAppleCallCount: Int = 0
    var lastSignUpInput: Pair<String, String>? = null
    var deleteAccountCallCount: Int = 0
    var signOutCallCount: Int = 0
    var lastSendPasswordResetEmailInput: String? = null

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<UserSession> {
        lastSignInInput = email to password
        return signInResult
    }

    override suspend fun signInWithGoogle(): Result<UserSession> {
        signInWithGoogleCallCount++
        return signInWithGoogleResult
    }

    override suspend fun signInWithApple(): Result<UserSession> {
        signInWithAppleCallCount++
        return signInWithAppleResult
    }

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<UserSession> {
        lastSignUpInput = email to password
        return signUpResult
    }

    override suspend fun deleteAccount(): Result<Unit> {
        deleteAccountCallCount++
        return deleteAccountResult
    }

    override suspend fun signOut(): Result<Unit> {
        signOutCallCount++
        return signOutResult
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        lastSendPasswordResetEmailInput = email
        return sendPasswordResetEmailResult
    }

    fun emitCurrentUser(userSession: UserSession?) {
        currentUserState.value = userSession
    }

    fun setSignInResult(result: Result<UserSession>) {
        signInResult = result
    }

    fun setSignInWithGoogleResult(result: Result<UserSession>) {
        signInWithGoogleResult = result
    }

    fun setSignInWithAppleResult(result: Result<UserSession>) {
        signInWithAppleResult = result
    }

    fun setSignUpResult(result: Result<UserSession>) {
        signUpResult = result
    }

    fun setDeleteAccountResult(result: Result<Unit>) {
        deleteAccountResult = result
    }

    fun setSignOutResult(result: Result<Unit>) {
        signOutResult = result
    }

    fun setSendPasswordResetEmailResult(result: Result<Unit>) {
        sendPasswordResetEmailResult = result
    }
}
