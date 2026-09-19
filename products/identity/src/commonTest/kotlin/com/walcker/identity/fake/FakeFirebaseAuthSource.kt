package com.walcker.identity.fake

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.FirebaseAuthSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeFirebaseAuthSource(
    initialUser: UserSession? = null,
    private var signInResult: Result<UserSession> = Result.failure(IllegalStateException("signIn not configured")),
    private var signUpResult: Result<UserSession> = Result.failure(IllegalStateException("signUp not configured")),
    private var deleteCurrentUserResult: Result<Unit> = Result.success(Unit),
    private var signOutResult: Result<Unit> = Result.success(Unit),
) : FirebaseAuthSource {
    private val currentUserState = MutableStateFlow(initialUser)

    override val currentUser: Flow<UserSession?> = currentUserState.asStateFlow()

    var lastSignInInput: Pair<String, String>? = null
    var lastSignUpInput: Pair<String, String>? = null
    var lastSignUpDisplayName: String? = null
    var deleteCurrentUserCallCount: Int = 0
    var signOutCallCount: Int = 0

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<UserSession> {
        lastSignInInput = email to password
        return signInResult
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserSession> {
        lastSignUpInput = email to password
        lastSignUpDisplayName = displayName
        return signUpResult
    }

    override suspend fun signOut(): Result<Unit> {
        signOutCallCount++
        return signOutResult
    }

    override suspend fun deleteCurrentUser(): Result<Unit> {
        deleteCurrentUserCallCount++
        return deleteCurrentUserResult
    }

    var lastReauthenticationPassword: String? = null
    var reauthenticateWithPasswordResult: Result<Unit> = Result.success(Unit)
    var signInProviderResult: Result<String?> = Result.success("password")

    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
        lastReauthenticationPassword = password
        return reauthenticateWithPasswordResult
    }

    override suspend fun signInProvider(): Result<String?> = signInProviderResult

    var lastSendPasswordResetEmailInput: String? = null
    private var sendPasswordResetEmailResult: Result<Unit> = Result.success(Unit)

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        lastSendPasswordResetEmailInput = email
        return sendPasswordResetEmailResult
    }

    var refreshSessionResult: Result<UserSession>? = null
    var refreshSessionCallCount: Int = 0
    var sendEmailVerificationResult: Result<Unit> = Result.success(Unit)
    var sendEmailVerificationCallCount: Int = 0

    override suspend fun refreshSession(): Result<UserSession> {
        refreshSessionCallCount++
        val result =
            refreshSessionResult
                ?: currentUserState.value?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("refreshSession without a user"))
        result.onSuccess { currentUserState.value = it }
        return result
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        sendEmailVerificationCallCount++
        return sendEmailVerificationResult
    }

    fun emitCurrentUser(userSession: UserSession?) {
        currentUserState.value = userSession
    }

    fun setSignInResult(result: Result<UserSession>) {
        signInResult = result
    }

    fun setSignUpResult(result: Result<UserSession>) {
        signUpResult = result
    }

    fun setDeleteCurrentUserResult(result: Result<Unit>) {
        deleteCurrentUserResult = result
    }

    fun setSignOutResult(result: Result<Unit>) {
        signOutResult = result
    }

    fun setSendPasswordResetEmailResult(result: Result<Unit>) {
        sendPasswordResetEmailResult = result
    }
}
