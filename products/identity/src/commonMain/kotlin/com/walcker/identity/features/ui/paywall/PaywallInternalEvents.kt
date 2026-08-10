package com.walcker.identity.features.ui.paywall

internal sealed interface PaywallInternalEvents {
    data class ShowSnackbar(
        val message: String,
    ) : PaywallInternalEvents

    data object Dismiss : PaywallInternalEvents
    data object RequireLogin : PaywallInternalEvents
}

