package com.walcker.identity.features.data.repository

import com.walcker.identity.features.data.remote.AccountDeletionCallableSource
import com.walcker.identity.features.domain.repository.AccountDeletionRepository

internal class AccountDeletionRepositoryImpl(
    private val source: AccountDeletionCallableSource,
) : AccountDeletionRepository {
    override suspend fun deleteRemoteData(): Result<Unit> = source.deleteRemoteData()
}
