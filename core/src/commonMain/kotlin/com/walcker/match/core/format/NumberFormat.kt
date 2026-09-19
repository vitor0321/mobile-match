package com.walcker.match.core.format

import kotlin.math.abs
import kotlin.math.floor

public fun formatDecimal(
    value: Float,
    decimals: Int = 1,
    decimalSeparator: Char = ',',
): String {
    require(decimals >= 0) { "decimals must be >= 0, was $decimals" }
    if (value.isNaN() || value.isInfinite()) return "-"

    var factor = 1L
    repeat(decimals) { factor *= 10L }

    val scaled = roundHalfUp(abs(value.toDouble()) * factor)
    val whole = scaled / factor
    val fraction = scaled % factor
    val sign = if (value < 0f && scaled != 0L) "-" else ""

    return if (decimals == 0) {
        "$sign$whole"
    } else {
        "$sign$whole$decimalSeparator${fraction.toString().padStart(decimals, '0')}"
    }
}

public fun formatPercent(fraction: Float): String {
    if (fraction.isNaN()) return "-"
    val clamped = fraction.coerceIn(0f, 1f)
    return "${roundHalfUp(clamped.toDouble() * 100.0)}%"
}

private fun roundHalfUp(value: Double): Long = floor(value + 0.5).toLong()
