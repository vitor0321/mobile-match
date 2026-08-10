package com.walcker.identity.features.data.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration

public object PurchasesBootstrap {

    public fun configure(apiKey: String) {
        if (apiKey.isBlank() || Purchases.isConfigured) return
        Purchases.configure(
            PurchasesConfiguration.Builder(apiKey)
                .build(),
        )
    }
}

