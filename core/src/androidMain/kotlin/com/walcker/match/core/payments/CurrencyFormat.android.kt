package com.walcker.match.core.payments

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

public actual fun currentDeviceCurrencyCode(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
        .getOrDefault(DEFAULT_CURRENCY_CODE)

public actual fun formatCurrencyCents(
    cents: Int,
    currencyCode: String,
): String {
    val currency =
        runCatching { Currency.getInstance(currencyCode) }
            .getOrDefault(Currency.getInstance(DEFAULT_CURRENCY_CODE))
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    format.currency = currency
    return format.format(cents / 100.0)
}

private const val DEFAULT_CURRENCY_CODE = "BRL"
