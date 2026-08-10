@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.walcker.identity.features.data.remote

import cocoapods.FirebaseFunctions.FIRFunctions
import com.walcker.identity.features.domain.usecase.RequiresRecentLoginException
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume

private const val FUNCTIONS_REGION = "southamerica-east1"

internal actual fun createAccountDeletionCallableSource(): AccountDeletionCallableSource =
    IosAccountDeletionCallableSource(FIRFunctions.functionsForRegion(FUNCTIONS_REGION))

private class IosAccountDeletionCallableSource(
    private val functions: FIRFunctions,
) : AccountDeletionCallableSource {
    override suspend fun deleteRemoteData(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        functions.HTTPSCallableWithName("deleteAccount").callWithCompletion { _, error: NSError? ->
            continuation.resume(
                if (error == null) Result.success(Unit)
                else Result.failure(error.toDeleteAccountThrowable()),
            )
        }
    }
}

private fun NSError.toDeleteAccountThrowable(): Throwable {
    val cause = IllegalStateException(localizedDescription)
    return if (code == 9L) RequiresRecentLoginException(cause) else cause
}
