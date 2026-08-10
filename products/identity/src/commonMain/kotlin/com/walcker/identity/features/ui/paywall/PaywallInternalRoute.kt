package com.walcker.identity.features.ui.paywall

internal sealed interface PaywallInternalRoute {
    data object OnBackClicked : PaywallInternalRoute
    data object OnPurchaseClicked : PaywallInternalRoute
    data object OnRestoreClicked : PaywallInternalRoute
    data object OnRetryClicked : PaywallInternalRoute
    data class OnOfferingSelected(
        val offeringId: String,
    ) : PaywallInternalRoute
}

