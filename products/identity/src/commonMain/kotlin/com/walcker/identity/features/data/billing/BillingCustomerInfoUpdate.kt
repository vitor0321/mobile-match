package com.walcker.identity.features.data.billing

/** A RevenueCat update explicitly associated with the Firebase account being reconciled. */
internal data class BillingCustomerInfoUpdate(
    val uid: String,
    val isPro: Boolean,
)
