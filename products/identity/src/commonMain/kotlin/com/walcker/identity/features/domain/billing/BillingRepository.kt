package com.walcker.identity.features.domain.billing

import kotlinx.collections.immutable.ImmutableList

internal interface BillingRepository {
    suspend fun listOfferings(): Result<ImmutableList<ProductOffering>>
    suspend fun purchase(packageId: String): Result<Unit>
    suspend fun restore(): Result<Boolean>
    suspend fun managementUrl(): Result<String?>
}

