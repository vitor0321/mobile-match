package com.walcker.identity.features.data.remote

internal interface VerificationSyncCallableSource {
    suspend fun syncVerificationStatus(): Result<Unit>
}

internal expect fun createVerificationSyncCallableSource(): VerificationSyncCallableSource
