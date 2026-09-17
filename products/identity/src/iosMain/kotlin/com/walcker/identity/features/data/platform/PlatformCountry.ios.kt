package com.walcker.identity.features.data.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale
import platform.Foundation.localizedStringForCountryCode

internal actual fun localizedCountryName(isoCode: String): String = NSLocale.currentLocale.localizedStringForCountryCode(isoCode)?.takeIf { it.isNotBlank() } ?: isoCode

internal actual fun deviceRegionIsoCode(): String? = (NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String)?.takeIf { it.length == 2 }
