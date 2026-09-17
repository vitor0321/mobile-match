@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.walcker.identity.features.data.remote

import cocoapods.FirebaseFunctions.FIRFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume

private const val FUNCTIONS_REGION = "southamerica-east1"
private const val SYNC_VERIFICATION_FUNCTION = "syncVerificationStatus"

internal actual fun createVerificationSyncCallableSource(): VerificationSyncCallableSource = IosVerificationSyncCallableSource(FIRFunctions.functionsForRegion(FUNCTIONS_REGION))

private class IosVerificationSyncCallableSource(
    private val functions: FIRFunctions,
) : VerificationSyncCallableSource {
    override suspend fun syncVerificationStatus(): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            functions
                .HTTPSCallableWithName(SYNC_VERIFICATION_FUNCTION)
                .callWithObject(emptyMap<Any?, Any?>()) { _, error: NSError? ->
                    continuation.resume(
                        if (error == null) Result.success(Unit) else Result.failure(IllegalStateException(error.localizedDescription)),
                    )
                }
        }
}
