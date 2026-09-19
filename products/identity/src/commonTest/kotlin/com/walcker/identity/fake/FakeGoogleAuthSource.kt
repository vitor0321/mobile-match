package com.walcker.identity.fake

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.GoogleAuthSource

internal class FakeGoogleAuthSource(
    private var signInResult: Result<UserSession> = Result.failure(IllegalStateException("signIn not configured")),
) : GoogleAuthSource {
    var signInCallCount: Int = 0
    var reauthenticateCallCount: Int = 0
    var reauthenticateResult: Result<Unit> = Result.success(Unit)

    override suspend fun signIn(): Result<UserSession> {
        signInCallCount++
        return signInResult
    }

    override suspend fun reauthenticate(): Result<Unit> {
        reauthenticateCallCount++
        return reauthenticateResult
    }

    fun setSignInResult(result: Result<UserSession>) {
        signInResult = result
    }
}
