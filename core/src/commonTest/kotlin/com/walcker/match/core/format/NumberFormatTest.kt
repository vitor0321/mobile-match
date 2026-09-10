package com.walcker.match.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {
    @Test
    fun `formatDecimal keeps one decimal by default`() {
        assertEquals("4,8", formatDecimal(4.8f))
    }

    @Test
    fun `formatDecimal honours the requested separator`() {
        assertEquals("4.8", formatDecimal(value = 4.8f, decimals = 1, decimalSeparator = '.'))
    }

    @Test
    fun `formatDecimal rounds half away from zero`() {
        assertEquals("4,3", formatDecimal(4.25f))
        assertEquals("5,0", formatDecimal(4.96f))
    }

    @Test
    fun `formatDecimal pads missing fraction digits`() {
        assertEquals("4,00", formatDecimal(value = 4f, decimals = 2))
        assertEquals("0,50", formatDecimal(value = 0.5f, decimals = 2))
    }

    @Test
    fun `formatDecimal without decimals drops the separator`() {
        assertEquals("5", formatDecimal(value = 4.6f, decimals = 0))
    }

    @Test
    fun `formatDecimal keeps the sign for negative values`() {
        assertEquals("-2,5", formatDecimal(-2.5f))
    }

    @Test
    fun `formatDecimal does not render a negative zero`() {
        assertEquals("0,0", formatDecimal(-0.01f))
    }

    @Test
    fun `formatDecimal degrades gracefully for non finite values`() {
        assertEquals("-", formatDecimal(Float.NaN))
        assertEquals("-", formatDecimal(Float.POSITIVE_INFINITY))
    }

    @Test
    fun `formatPercent converts a fraction to a rounded percentage`() {
        assertEquals("85%", formatPercent(0.851f))
        assertEquals("0%", formatPercent(0f))
    }

    @Test
    fun `formatPercent clamps values outside the valid range`() {
        assertEquals("100%", formatPercent(4.2f))
        assertEquals("0%", formatPercent(-1f))
    }
}
