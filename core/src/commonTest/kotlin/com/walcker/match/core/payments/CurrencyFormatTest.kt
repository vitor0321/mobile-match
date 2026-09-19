package com.walcker.match.core.payments

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {
    @Test
    fun `zero cents formats as R$ 0,00`() {
        assertEquals("R$ 0,00", formatBRLCents(0))
    }

    @Test
    fun `cents under one real are left-padded to two digits`() {
        assertEquals("R$ 0,09", formatBRLCents(9))
        assertEquals("R$ 0,90", formatBRLCents(90))
    }

    @Test
    fun `a plain value under one thousand has no thousands separator`() {
        assertEquals("R$ 1,50", formatBRLCents(150))
        assertEquals("R$ 999,99", formatBRLCents(99_999))
    }

    @Test
    fun `exactly one thousand reais gets a single thousands separator`() {
        assertEquals("R$ 1.000,00", formatBRLCents(100_000))
    }

    @Test
    fun `values in the low thousands separate correctly`() {
        assertEquals("R$ 1.234,56", formatBRLCents(123_456))
    }

    @Test
    fun `values past one million get two thousands separators`() {
        assertEquals("R$ 1.000.000,00", formatBRLCents(100_000_000))
        assertEquals("R$ 1.234.567,89", formatBRLCents(123_456_789))
    }
}
