package com.walcker.match.firestore

import com.google.firebase.Timestamp
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimestampNormalizerAndroidTest {
    @Test
    fun `a Firebase Timestamp is converted to epoch millis`() {
        val timestamp = Timestamp(1_760_000_000L, 500_000_000)

        val millis = normalizeTimestampMillis(timestamp)

        assertEquals(timestamp.toDate().time, millis)
    }

    @Test
    fun `a java Date is converted to epoch millis`() {
        val date = Date(1_760_000_000_000L)

        assertEquals(1_760_000_000_000L, normalizeTimestampMillis(date))
    }

    @Test
    fun `a raw numeric value falls back to numeric normalization`() {
        assertEquals(1_760_000_000_000L, normalizeTimestampMillis(1_760_000_000_000L))
        assertEquals(42L, normalizeTimestampMillis(42))
    }

    @Test
    fun `null yields null`() {
        assertNull(normalizeTimestampMillis(null))
    }

    @Test
    fun `an unsupported type yields null instead of throwing`() {
        assertNull(normalizeTimestampMillis("not a timestamp"))
    }
}
