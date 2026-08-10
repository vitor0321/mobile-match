package com.walcker.identity.fake

import com.walcker.identity.features.data.billing.BillingClient
import com.walcker.identity.features.data.billing.BillingCustomerInfoUpdate
import com.walcker.identity.features.domain.billing.ProductOffering
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FakeBillingClient(
    private var logInResult: Result<Boolean> = Result.success(false),
    private var logOutResult: Result<Unit> = Result.success(Unit),
    private var offeringsResult: Result<ImmutableList<ProductOffering>> = Result.success(persistentListOf()),
    private var purchaseResult: Result<Boolean> = Result.success(false),
    private var restoreResult: Result<Boolean> = Result.success(false),
    private var managementUrlResult: Result<String?> = Result.success(null),
) : BillingClient {
    private val customerInfoUpdatesState = MutableSharedFlow<BillingCustomerInfoUpdate>(replay = 1)

    var lastLoggedInUserId: String? = null
    var logOutCallCount: Int = 0
    var purchasedPackageIds: List<String> = emptyList()
        private set
    var restoreCallCount: Int = 0
        private set
    var managementUrlCallCount: Int = 0
        private set

    override fun customerInfoUpdates(): Flow<BillingCustomerInfoUpdate> = customerInfoUpdatesState

    override suspend fun logIn(userId: String): Result<Boolean> {
        lastLoggedInUserId = userId
        return logInResult
    }

    override suspend fun logOut(): Result<Unit> {
        logOutCallCount++
        return logOutResult
    }

    override suspend fun listOfferings(): Result<ImmutableList<ProductOffering>> = offeringsResult

    override suspend fun purchase(packageId: String): Result<Boolean> {
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

    fun setManagementUrlResult(result: Result<String?>) {
        managementUrlResult = result
    }

    suspend fun emitCustomerInfoUpdate(
        isPro: Boolean,
        uid: String = requireNotNull(lastLoggedInUserId),
    ) {
        customerInfoUpdatesState.emit(BillingCustomerInfoUpdate(uid, isPro))
    }

    fun setLogInResult(result: Result<Boolean>) {
        logInResult = result
    }

    fun setOfferingsResult(result: Result<ImmutableList<ProductOffering>>) {
        offeringsResult = result
    }

    fun setPurchaseResult(result: Result<Boolean>) {
        purchaseResult = result
    }

    fun setRestoreResult(result: Result<Boolean>) {
        restoreResult = result
    }
}

