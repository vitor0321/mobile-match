package com.walcker.identity.features.data.remote

internal interface AccountDeletionCallableSource {
    suspend fun deleteRemoteData(): Result<Unit>
}

internal expect fun createAccountDeletionCallableSource(): AccountDeletionCallableSource
