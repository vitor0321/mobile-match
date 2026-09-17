package com.walcker.identity.features.data.platform

import java.util.Locale

internal actual fun localizedCountryName(isoCode: String): String = Locale("", isoCode).displayCountry.ifBlank { isoCode }

internal actual fun deviceRegionIsoCode(): String? = Locale.getDefault().country.takeIf { it.length == 2 }
