package com.walcker.identity.features.domain.phone

private const val BRAZIL_NATIONAL_DIGITS_MIN = 10
private const val BRAZIL_NATIONAL_DIGITS_MAX = 11
private const val NATIONAL_DIGITS_MIN = 6
private const val NATIONAL_DIGITS_MAX = 14
private const val E164_DIGITS_MAX = 15
private const val REGIONAL_INDICATOR_A = 0x1F1E6
private const val SUPPLEMENTARY_PLANE_START = 0x10000
private const val HIGH_SURROGATE_START = 0xD800
private const val LOW_SURROGATE_START = 0xDC00
private const val SURROGATE_SHIFT = 10
private const val LOW_SURROGATE_MASK = 0x3FF

internal fun digitsOnly(value: String): String =
    buildString {
        for (character in value) {
            val digit = character.digitToIntOrNull() ?: continue
            append('0' + digit)
        }
    }

internal fun nationalDigits(
    country: CountryDialCode,
    value: String,
): String {
    val digits = digitsOnly(value)
    val withoutDialCode =
        if (value.trimStart().startsWith('+') && digits.startsWith(country.dialCode)) {
            digits.removePrefix(country.dialCode)
        } else {
            digits
        }
    return if (country.dialCode == BRAZIL_DIAL_CODE) withoutDialCode.trimStart('0') else withoutDialCode
}

internal fun maxNationalDigits(country: CountryDialCode): Int =
    if (country.dialCode == BRAZIL_DIAL_CODE) {
        BRAZIL_NATIONAL_DIGITS_MAX
    } else {
        minOf(NATIONAL_DIGITS_MAX, E164_DIGITS_MAX - country.dialCode.length)
    }

internal fun toE164(
    country: CountryDialCode,
    nationalNumber: String,
): String? {
    val national = nationalDigits(country, nationalNumber)
    val range =
        if (country.dialCode == BRAZIL_DIAL_CODE) {
            BRAZIL_NATIONAL_DIGITS_MIN..BRAZIL_NATIONAL_DIGITS_MAX
        } else {
            NATIONAL_DIGITS_MIN..NATIONAL_DIGITS_MAX
        }
    if (national.length !in range) return null
    if (country.dialCode.length + national.length > E164_DIGITS_MAX) return null
    return "+${country.dialCode}$national"
}

internal fun formatBrazilianNational(value: String): String {
    val digits = digitsOnly(value).take(BRAZIL_NATIONAL_DIGITS_MAX)
    return when {
        digits.isEmpty() -> ""
        digits.length <= 2 -> "($digits"
        digits.length <= 6 -> "(${digits.take(2)}) ${digits.drop(2)}"
        digits.length <= 10 -> "(${digits.take(2)}) ${digits.substring(2, 6)}-${digits.drop(6)}"
        else -> "(${digits.take(2)}) ${digits.substring(2, 7)}-${digits.drop(7)}"
    }
}

internal fun flagEmoji(isoCode: String): String {
    val normalized = isoCode.uppercase()
    if (normalized.length != 2 || !normalized.all { it in 'A'..'Z' }) return ""
    return normalized
        .map { letter -> codePointToString(REGIONAL_INDICATOR_A + (letter - 'A')) }
        .joinToString(separator = "")
}

private fun codePointToString(codePoint: Int): String {
    val offset = codePoint - SUPPLEMENTARY_PLANE_START
    val high = (HIGH_SURROGATE_START + (offset shr SURROGATE_SHIFT)).toChar()
    val low = (LOW_SURROGATE_START + (offset and LOW_SURROGATE_MASK)).toChar()
    return charArrayOf(high, low).concatToString()
}
