package com.walcker.identity.features.domain.billing

internal sealed class PurchaseError(
    message: String,
) : Exception(message) {
    data object UserCancelled : PurchaseError("Purchase was cancelled.")
    data object Network : PurchaseError("A network error happened while contacting billing.")
    data object ProductUnavailable : PurchaseError("The selected product is not available anymore.")
    data object BillingUnavailable : PurchaseError("Billing is not available in the current app setup.")
    data class Unknown(
        private val details: String?,
    ) : PurchaseError(details ?: "An unexpected billing error happened.")
}

