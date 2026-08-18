package com.walcker.match.core.format

import kotlin.math.abs
import kotlin.math.floor

/**
 * Multiplatform decimal formatting.
 *
 * `String.format` is JVM-only and does not compile for Kotlin/Native, so every
 * shared screen must go through helpers like this one instead.
 *
 * Ties round away from zero (`4.25 -> 4.3`), which is what people expect from a
 * rating. `kotlin.math.round` rounds ties to even and would show `4.2`.
 *
 * @param value the number to format
 * @param decimals how many digits to keep after the separator (>= 0)
 * @param decimalSeparator `,` for pt-BR, `.` for en-US — the strings layer decides
 */
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

/**
 * Formats a `0f..1f` fraction as a rounded integer percentage: `0.85f` -> `"85%"`.
 *
 * Values outside the range are clamped so a malformed backend value can never
 * render something like `-400%`.
 */
public fun formatPercent(fraction: Float): String {
    if (fraction.isNaN()) return "-"
    val clamped = fraction.coerceIn(0f, 1f)
    return "${roundHalfUp(clamped.toDouble() * 100.0)}%"
}

/** Half-up rounding for a non-negative value. */
private fun roundHalfUp(value: Double): Long = floor(value + 0.5).toLong()
