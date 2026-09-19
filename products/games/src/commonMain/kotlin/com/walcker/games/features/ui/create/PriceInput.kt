package com.walcker.games.features.ui.create

internal const val MAX_PRICE_CENTS = 100_000
private const val CENTS_PER_UNIT = 100
private val PRICE_PATTERN = Regex("""^(\d{1,4})(?:[.,](\d{0,2}))?$""")

internal fun parsePriceCents(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    val match = PRICE_PATTERN.matchEntire(trimmed) ?: return null
    val whole = match.groupValues[1].toInt()
    val fraction = match.groupValues[2].padEnd(2, '0').toInt()
    return (whole * CENTS_PER_UNIT + fraction).takeIf { it <= MAX_PRICE_CENTS }
}

internal fun priceTextFromCents(cents: Int): String =
    if (cents <= 0) {
        ""
    } else {
        "${cents / CENTS_PER_UNIT}.${(cents % CENTS_PER_UNIT).toString().padStart(2, '0')}"
    }
