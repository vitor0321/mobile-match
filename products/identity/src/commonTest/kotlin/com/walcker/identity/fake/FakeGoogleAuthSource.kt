package com.walcker.identity.fake

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.GoogleAuthSource

internal class FakeGoogleAuthSource(
    private var signInResult: Result<UserSession> = Result.failure(IllegalStateException("signIn not configured")),
) : GoogleAuthSource {
    var signInCallCount: Int = 0

    override suspend fun signIn(): Result<UserSession> {
        signInCallCount++
        return signInResult
    }

    fun setSignInResult(result: Result<UserSession>) {
        signInResult = result
    }
}
