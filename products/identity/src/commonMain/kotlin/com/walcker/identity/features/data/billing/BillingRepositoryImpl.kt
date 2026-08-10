package com.walcker.identity.features.data.billing

import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.walcker.identity.features.domain.billing.BillingRepository
import com.walcker.identity.features.domain.billing.ProductOffering
import com.walcker.identity.features.domain.billing.PurchaseError
import kotlinx.collections.immutable.ImmutableList

internal class BillingRepositoryImpl(
    private val billingClient: BillingClient,
) : BillingRepository {
    override suspend fun listOfferings(): Result<ImmutableList<ProductOffering>> {
        return billingClient.listOfferings().mapError()
    }

    override suspend fun purchase(packageId: String): Result<Unit> {
        return billingClient.purchase(packageId)
            .mapError()
            .fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(it) },
            )
    }

    override suspend fun restore(): Result<Boolean> {
        return billingClient.restore().mapError()
    }

    override suspend fun managementUrl(): Result<String?> {
        return billingClient.managementUrl().mapError()
    }
}

private fun <T> Result<T>.mapError(): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(it.toPurchaseError()) },
    )
}

private fun Throwable.toPurchaseError(): PurchaseError {
    return when (this) {
        is PurchaseError -> this
        is PurchasesTransactionException -> {
            if (userCancelled) {
                PurchaseError.UserCancelled
            } else {
                toErrorByCode(code, message)
            }
        }

        is PurchasesException -> toErrorByCode(code, message)
        is IllegalStateException -> {
            if (message?.contains("configured", ignoreCase = true) == true) {
                PurchaseError.BillingUnavailable
            } else {
                PurchaseError.Unknown(message)
            }
        }

        else -> PurchaseError.Unknown(message)
    }
}

private fun toErrorByCode(
    code: PurchasesErrorCode,
    message: String?,
): PurchaseError {
    return when (code) {
        PurchasesErrorCode.NetworkError,
        PurchasesErrorCode.OfflineConnectionError,
        -> PurchaseError.Network

        PurchasesErrorCode.ProductNotAvailableForPurchaseError,
        PurchasesErrorCode.ProductAlreadyPurchasedError,
        PurchasesErrorCode.PurchaseInvalidError,
        -> PurchaseError.ProductUnavailable

        PurchasesErrorCode.InvalidCredentialsError,
        PurchasesErrorCode.ConfigurationError,
        PurchasesErrorCode.UnsupportedError,
        -> PurchaseError.BillingUnavailable

        else -> PurchaseError.Unknown(message)
    }
}

