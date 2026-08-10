package com.walcker.identity.features.ui.paywall

internal sealed interface PaywallError {
    data object PurchaseCancelled : PaywallError
    data object Network : PaywallError
    data object ProductUnavailable : PaywallError
    data object BillingUnavailable : PaywallError
    data class Generic(
        val message: String,
    ) : PaywallError
}

