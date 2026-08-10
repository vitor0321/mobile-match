package com.walcker.identity.features.domain.billing

internal data class ProductOffering(
    val id: String,
    val offeringId: String,
    val packageId: String,
    val title: String,
    val description: String,
    val priceLabel: String,
)

