package com.walcker.identity.fake

import com.walcker.identity.features.domain.repository.AccountDeletionRepository

internal class FakeAccountDeletionRepository(
    private var result: Result<Unit> = Result.success(Unit),
) : AccountDeletionRepository {
    var callCount: Int = 0
        private set

    override suspend fun deleteRemoteData(): Result<Unit> {
        callCount++
        return result
    }

    fun setResult(result: Result<Unit>) {
        this.result = result
    }
}
