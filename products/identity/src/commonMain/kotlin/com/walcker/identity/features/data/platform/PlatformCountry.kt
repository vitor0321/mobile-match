package com.walcker.identity.features.data.platform

internal expect fun localizedCountryName(isoCode: String): String

internal expect fun deviceRegionIsoCode(): String?
