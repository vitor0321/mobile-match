package com.walcker.identity.features.ui.paywall

import com.walcker.identity.features.domain.billing.ProductOffering
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class PaywallState(
    val isLoading: Boolean = true,
    val isRestoring: Boolean = false,
    val purchaseInProgress: String? = null,
    val offerings: ImmutableList<ProductOffering> = persistentListOf(),
    val selectedOfferingId: String? = null,
    val isPro: Boolean = false,
    val selectedOffering: ProductOffering? = null,
    val selectedOfferingPeriod: PaywallOfferingPeriod? = null,
    val managementUrl: String? = null,
    val error: PaywallError? = null,
    val errorMessage: String? = null,
)


internal enum class PaywallOfferingPeriod {
    MONTHLY,
    YEARLY,
}