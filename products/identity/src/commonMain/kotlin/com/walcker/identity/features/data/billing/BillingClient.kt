package com.walcker.identity.features.data.billing

import com.walcker.identity.features.domain.billing.ProductOffering
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

internal interface BillingClient {
    fun customerInfoUpdates(): Flow<BillingCustomerInfoUpdate>

    suspend fun logIn(userId: String): Result<Boolean>
    suspend fun logOut(): Result<Unit>
    suspend fun listOfferings(): Result<ImmutableList<ProductOffering>>
    suspend fun purchase(packageId: String): Result<Boolean>
    suspend fun restore(): Result<Boolean>
    suspend fun managementUrl(): Result<String?>
}

