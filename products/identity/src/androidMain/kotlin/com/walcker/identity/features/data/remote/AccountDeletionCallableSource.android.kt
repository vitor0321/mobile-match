package com.walcker.identity.features.data.remote

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.walcker.identity.features.domain.usecase.RequiresRecentLoginException
import kotlinx.coroutines.tasks.await

private const val FUNCTIONS_REGION = "southamerica-east1"

internal actual fun createAccountDeletionCallableSource(): AccountDeletionCallableSource =
    AndroidAccountDeletionCallableSource(FirebaseFunctions.getInstance(FUNCTIONS_REGION))

private class AndroidAccountDeletionCallableSource(
    private val functions: FirebaseFunctions,
) : AccountDeletionCallableSource {
    override suspend fun deleteRemoteData(): Result<Unit> = runCatching {
        functions
            .getHttpsCallable("deleteAccount")
            .call()
            .await()
        Unit
    }.recoverCatching { error ->
        if (error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
            throw RequiresRecentLoginException(error)
        }
        throw error
    }
}
