package com.walcker.match.firestore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimestampNormalizerTest {
    @Test
    fun `a Long value passes through unchanged`() {
        assertEquals(1_760_000_000_000L, normalizeNumericTimestamp(1_760_000_000_000L))
    }

    @Test
    fun `an Int value is widened to Long`() {
        assertEquals(42L, normalizeNumericTimestamp(42))
    }

    @Test
    fun `a Double value is truncated to Long`() {
        assertEquals(1_760_000_000_123L, normalizeNumericTimestamp(1_760_000_000_123.9))
    }

    @Test
    fun `a Float value is truncated to Long`() {
        assertEquals(42L, normalizeNumericTimestamp(42.9f))
    }

    @Test
    fun `null yields null`() {
        assertNull(normalizeNumericTimestamp(null))
    }

    @Test
    fun `an unsupported type yields null instead of throwing`() {
        assertNull(normalizeNumericTimestamp("not a timestamp"))
        assertNull(normalizeNumericTimestamp(listOf(1, 2, 3)))
    }
}
