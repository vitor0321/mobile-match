package com.walcker.match.core.payments

/**
 * Formats a value in cents as pt-BR BRL currency.
 *
 * Ported from `formatBRLCents` in `lib/pix.ts` (Lovable MVP).
 */
public fun formatBRLCents(cents: Int): String {
    val reais = cents / 100
    val centavos = cents % 100
    val reaisStr = reais.toLong().let { value ->
        val builder = StringBuilder()
        var remaining = value
        while (remaining >= 1000) {
            builder.insert(0, ".${"%03d".format(remaining % 1000)}")
            remaining /= 1000
        }
        builder.insert(0, remaining.toString())
        builder.toString()
    }
    return "R$ ${reaisStr},${"%02d".format(centavos)}"
}
