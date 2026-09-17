package com.walcker.identity.fake

import com.walcker.identity.features.data.remote.VerificationSyncCallableSource

internal class FakeVerificationSyncCallableSource(
    var result: Result<Unit> = Result.success(Unit),
) : VerificationSyncCallableSource {
    var callCount: Int = 0
        private set

    override suspend fun syncVerificationStatus(): Result<Unit> {
        callCount++
        return result
    }
}
