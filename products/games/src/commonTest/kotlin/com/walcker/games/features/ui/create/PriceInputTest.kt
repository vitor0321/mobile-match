package com.walcker.games.features.ui.create

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceInputTest {
    @Test
    fun `a comma and a dot are both decimal separators`() {
        assertEquals(1250, parsePriceCents("12,50"))
        assertEquals(1250, parsePriceCents("12.50"))
    }

    @Test
    fun `whole amounts and a single decimal digit are accepted`() {
        assertEquals(2500, parsePriceCents("25"))
        assertEquals(2550, parsePriceCents("25,5"))
    }

    @Test
    fun `a separator still being typed does not count as an error`() {
        assertEquals(1200, parsePriceCents("12,"))
    }

    @Test
    fun `an empty price is a free match`() {
        assertEquals(0, parsePriceCents(""))
        assertEquals(0, parsePriceCents("   "))
    }

    @Test
    fun `surrounding spaces are ignored`() {
        assertEquals(700, parsePriceCents(" 7 "))
    }

    @Test
    fun `the limit the security rules enforce is accepted and nothing above it`() {
        assertEquals(100_000, parsePriceCents("1000"))
        assertEquals(100_000, parsePriceCents("1000,00"))
        assertNull(parsePriceCents("1000,01"))
        assertNull(parsePriceCents("1500"))
    }

    @Test
    fun `anything that is not a plain amount is rejected`() {
        for (text in listOf("-5", "abc", "NaN", "Infinity", "1.234,50", "12,345", "12,5,0", ",50", "R$ 10", "1e3")) {
            assertNull(parsePriceCents(text), "'$text' should be rejected")
        }
    }

    @Test
    fun `cents become the text the price field shows`() {
        assertEquals("15.50", priceTextFromCents(1550))
        assertEquals("15.05", priceTextFromCents(1505))
        assertEquals("20.00", priceTextFromCents(2000))
        assertEquals("", priceTextFromCents(0))
    }
}
