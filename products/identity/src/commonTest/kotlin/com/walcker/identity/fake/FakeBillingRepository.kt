package com.walcker.identity.fake

import com.walcker.identity.features.domain.billing.BillingRepository
import com.walcker.identity.features.domain.billing.ProductOffering
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class FakeBillingRepository(
    private var offeringsResult: Result<ImmutableList<ProductOffering>> = Result.success(persistentListOf()),
    private var purchaseResult: Result<Unit> = Result.success(Unit),
    private var restoreResult: Result<Boolean> = Result.success(false),
    private var managementUrlResult: Result<String?> = Result.success(null),
) : BillingRepository {
    var purchasedPackageIds: List<String> = emptyList()
        private set
    var restoreCallCount: Int = 0
        private set
    var managementUrlCallCount: Int = 0
        private set

    override suspend fun listOfferings(): Result<ImmutableList<ProductOffering>> = offeringsResult

    override suspend fun purchase(packageId: String): Result<Unit> {
        purchasedPackageIds = purchasedPackageIds + packageId
        return purchaseResult
    }

    override suspend fun restore(): Result<Boolean> {
        restoreCallCount++
        return restoreResult
    }

    override suspend fun managementUrl(): Result<String?> {
        managementUrlCallCount++
        return managementUrlResult
    }

    fun setOfferingsResult(result: Result<ImmutableList<ProductOffering>>) {
        offeringsResult = result
    }

    fun setPurchaseResult(result: Result<Unit>) {
        purchaseResult = result
    }

    fun setRestoreResult(result: Result<Boolean>) {
        restoreResult = result
    }

    fun setManagementUrlResult(result: Result<String?>) {
        managementUrlResult = result
    }
}

