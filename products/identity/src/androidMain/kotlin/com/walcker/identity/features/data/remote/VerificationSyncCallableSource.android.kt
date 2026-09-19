package com.walcker.identity.features.data.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

private const val FUNCTIONS_REGION = "southamerica-east1"
private const val SYNC_VERIFICATION_FUNCTION = "syncVerificationStatus"

internal actual fun createVerificationSyncCallableSource(): VerificationSyncCallableSource = AndroidVerificationSyncCallableSource(FirebaseFunctions.getInstance(FUNCTIONS_REGION))

private class AndroidVerificationSyncCallableSource(
    private val functions: FirebaseFunctions,
) : VerificationSyncCallableSource {
    override suspend fun syncVerificationStatus(): Result<Unit> =
        runCatching {
            functions
                .getHttpsCallable(SYNC_VERIFICATION_FUNCTION)
                .call(emptyMap<String, Any>())
                .await()
            Unit
        }.onFailure { error -> if (error is CancellationException) throw error }
}
