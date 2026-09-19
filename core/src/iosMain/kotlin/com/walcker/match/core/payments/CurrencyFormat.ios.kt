package com.walcker.match.core.payments

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.currencyCode
import platform.Foundation.currentLocale

private const val DEFAULT_CURRENCY_CODE = "BRL"

public actual fun currentDeviceCurrencyCode(): String = NSLocale.currentLocale.currencyCode ?: DEFAULT_CURRENCY_CODE

public actual fun formatCurrencyCents(
    cents: Int,
    currencyCode: String,
): String {
    val formatter =
        NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterCurrencyStyle
            this.currencyCode = currencyCode
        }
    return formatter.stringFromNumber(NSNumber(cents / 100.0))
        ?: "$currencyCode ${cents / 100.0}"
}
