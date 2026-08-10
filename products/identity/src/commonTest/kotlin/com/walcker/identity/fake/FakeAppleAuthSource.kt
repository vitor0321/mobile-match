package com.walcker.identity.fake

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.AppleAuthSource

internal class FakeAppleAuthSource(
    private var signInResult: Result<UserSession> = Result.failure(IllegalStateException("signIn not configured")),
) : AppleAuthSource {
    var signInCallCount: Int = 0

    override suspend fun signIn(): Result<UserSession> {
        signInCallCount++
        return signInResult
    }

    fun setSignInResult(result: Result<UserSession>) {
        signInResult = result
    }
}
