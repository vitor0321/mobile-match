package com.walcker.identity.features.data.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.walcker.identity.features.domain.billing.ProductOffering
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.coroutines.resume

private const val PRO_ENTITLEMENT_ID = "Match Pro"
private const val BILLING_NOT_CONFIGURED_MESSAGE = "RevenueCat has not been configured."

internal class RevenueCatBillingClient : BillingClient {
    private val customerInfoUpdatesState = MutableSharedFlow<BillingCustomerInfoUpdate>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    private var delegateAttached = false
    private var activeUserId: String? = null

    override fun customerInfoUpdates(): Flow<BillingCustomerInfoUpdate> {
        if (Purchases.isConfigured) attachDelegateIfNeeded()
        return customerInfoUpdatesState.distinctUntilChanged()
    }

    override suspend fun logIn(userId: String): Result<Boolean> {
        return runBillingCall {
            ensureConfigured()
            attachDelegateIfNeeded()
            val customerInfo = Purchases.sharedInstance.awaitLogIn(userId).first
            activeUserId = userId
            customerInfo.hasProAccess()
        }
    }

    override suspend fun logOut(): Result<Unit> {
        if (!Purchases.isConfigured) return Result.success(Unit)
        attachDelegateIfNeeded()
        val uid = activeUserId
        activeUserId = null
        return runBillingCall {
            Purchases.sharedInstance.awaitLogOut()
            if (uid != null) customerInfoUpdatesState.tryEmit(BillingCustomerInfoUpdate(uid, false))
        }
    }

    override suspend fun listOfferings(): Result<ImmutableList<ProductOffering>> {
        return runBillingCall {
            ensureConfigured()
            attachDelegateIfNeeded()
            Purchases.sharedInstance.awaitOfferings()
                .current
                ?.availablePackages
                .orEmpty()
                .sortedBy { it.packageType.rank() }
                .map { it.toProductOffering() }
                .toImmutableList()
        }
    }

    override suspend fun purchase(packageId: String): Result<Boolean> {
        return runBillingCall {
            ensureConfigured()
            attachDelegateIfNeeded()
            val packageToPurchase = resolvePackage(packageId)
            Purchases.sharedInstance.awaitPurchase(packageToPurchase).second.hasProAccess()
        }
    }

    override suspend fun restore(): Result<Boolean> {
        return runBillingCall {
            ensureConfigured()
            attachDelegateIfNeeded()
            Purchases.sharedInstance.awaitRestore().hasProAccess()
        }
    }

    override suspend fun managementUrl(): Result<String?> {
        return runBillingCall {
            ensureConfigured()
            attachDelegateIfNeeded()
            Purchases.sharedInstance.awaitCustomerInfo().managementUrlString
        }
    }

    private fun attachDelegateIfNeeded() {
        if (delegateAttached) return
        Purchases.sharedInstance.delegate = object : PurchasesDelegate {
            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: (
                    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                    onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
                ) -> Unit,
            ) = Unit

            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                activeUserId?.let { uid ->
                    customerInfoUpdatesState.tryEmit(BillingCustomerInfoUpdate(uid, customerInfo.hasProAccess()))
                }
            }
        }
        delegateAttached = true
    }

    private suspend fun resolvePackage(packageId: String): Package {
        val offerings = Purchases.sharedInstance.awaitOfferings()
        val packages = buildList<Package> {
            offerings.current?.availablePackages?.let { addAll(it) }
            offerings.all.forEach { (_, availableOffering) ->
                addAll(availableOffering.availablePackages)
            }
        }
        return packages.firstOrNull { availablePackage ->
            availablePackage.identifier == packageId
        } ?: error("Requested offering is no longer available.")
    }

    private fun ensureConfigured() {
        check(Purchases.isConfigured) { BILLING_NOT_CONFIGURED_MESSAGE }
    }
}

private fun CustomerInfo.hasProAccess(): Boolean {
    return entitlements.active[PRO_ENTITLEMENT_ID]?.isActive == true
}

private suspend inline fun <T> runBillingCall(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private fun Package.toProductOffering(): ProductOffering {
    return ProductOffering(
        id = "${presentedOfferingContext.offeringIdentifier}:$identifier",
        offeringId = presentedOfferingContext.offeringIdentifier,
        packageId = identifier,
        title = storeProduct.title.trim(),
        description = storeProduct.localizedDescription?.takeIf { it.isNotBlank() }
            ?: defaultDescription(),
        priceLabel = storeProduct.price.formatted,
    )
}

private fun Package.defaultDescription(): String {
    return when (packageType) {
        PackageType.LIFETIME -> "Lifetime access"
        PackageType.ANNUAL -> "Annual plan"
        PackageType.SIX_MONTH -> "6-month plan"
        PackageType.THREE_MONTH -> "3-month plan"
        PackageType.TWO_MONTH -> "2-month plan"
        PackageType.MONTHLY -> "Monthly plan"
        PackageType.WEEKLY -> "Weekly plan"
        else -> identifier
    }
}

private fun PackageType.rank(): Int {
    return when (this) {
        PackageType.LIFETIME -> 0
        PackageType.ANNUAL -> 1
        PackageType.SIX_MONTH -> 2
        PackageType.THREE_MONTH -> 3
        PackageType.TWO_MONTH -> 4
        PackageType.MONTHLY -> 5
        PackageType.WEEKLY -> 6
        else -> 7
    }
}


private suspend fun Purchases.awaitOfferings(): Offerings {
    return suspendCancellableCoroutine { continuation ->
        getOfferings(
            onError = { error -> continuation.resumeWith(Result.failure(error.asException())) },
            onSuccess = { offerings -> continuation.resume(offerings) },
        )
    }
}

private suspend fun Purchases.awaitLogIn(userId: String): Pair<CustomerInfo, Boolean> {
    return suspendCancellableCoroutine { continuation ->
        logIn(
            newAppUserID = userId,
            onError = { error -> continuation.resumeWith(Result.failure(error.asException())) },
            onSuccess = { customerInfo, created -> continuation.resume(customerInfo to created) },
        )
    }
}

private suspend fun Purchases.awaitLogOut(): CustomerInfo {
    return suspendCancellableCoroutine { continuation ->
        logOut(
            onError = { error -> continuation.resumeWith(Result.failure(error.asException())) },
            onSuccess = { customerInfo -> continuation.resume(customerInfo) },
        )
    }
}

private suspend fun Purchases.awaitPurchase(packageToPurchase: Package): Pair<StoreTransaction, CustomerInfo> {
    return suspendCancellableCoroutine { continuation ->
        purchase(
            packageToPurchase = packageToPurchase,
            onError = { error, userCancelled ->
                continuation.resumeWith(Result.failure(error.asTransactionException(userCancelled)))
            },
            onSuccess = { transaction, customerInfo -> continuation.resume(transaction to customerInfo) },
        )
    }
}

private suspend fun Purchases.awaitRestore(): CustomerInfo {
    return suspendCancellableCoroutine { continuation ->
        restorePurchases(
            onError = { error -> continuation.resumeWith(Result.failure(error.asException())) },
            onSuccess = { customerInfo -> continuation.resume(customerInfo) },
        )
    }
}

private suspend fun Purchases.awaitCustomerInfo(): CustomerInfo {
    return suspendCancellableCoroutine { continuation ->
        getCustomerInfo(
            onError = { error -> continuation.resumeWith(Result.failure(error.asException())) },
            onSuccess = { customerInfo -> continuation.resume(customerInfo) },
        )
    }
}

private fun PurchasesError.asException(): Throwable {
    return com.revenuecat.purchases.kmp.models.PurchasesException(this)
}

private fun PurchasesError.asTransactionException(userCancelled: Boolean): Throwable {
    return com.revenuecat.purchases.kmp.models.PurchasesTransactionException(this, userCancelled)
}

