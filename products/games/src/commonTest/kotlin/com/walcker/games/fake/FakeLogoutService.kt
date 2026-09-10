package com.walcker.games.fake

import com.walcker.identity.api.LogoutService

internal class FakeLogoutService(
    var result: Result<Unit> = Result.success(Unit),
) : LogoutService {
    var logoutCallCount: Int = 0
        private set

    override suspend fun logout(): Result<Unit> {
        logoutCallCount++
        return result
    }
}
