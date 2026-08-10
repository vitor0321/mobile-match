package com.walcker.identity.features.domain.repository

internal interface AccountDeletionRepository {
    suspend fun deleteRemoteData(): Result<Unit>
}
