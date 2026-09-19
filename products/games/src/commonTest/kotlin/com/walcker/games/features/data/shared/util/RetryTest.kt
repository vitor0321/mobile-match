package com.walcker.games.features.data.shared.util

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryTest {
    @Test
    fun `returns the result on the first successful attempt`() =
        runTest {
            var calls = 0

            val result =
                withRetry {
                    calls++
                    "ok"
                }

            assertEquals("ok", result)
            assertEquals(1, calls)
        }

    @Test
    fun `retries after a failure and succeeds within the attempt budget`() =
        runTest {
            var calls = 0

            val result =
                withRetry(maxAttempts = 3, initialDelayMs = 1L) {
                    calls++
                    if (calls < 3) error("still failing")
                    "ok"
                }

            assertEquals("ok", result)
            assertEquals(3, calls)
        }

    @Test
    fun `gives up and rethrows once every attempt is spent`() =
        runTest {
            var calls = 0

            assertFailsWith<IllegalStateException> {
                withRetry(maxAttempts = 3, initialDelayMs = 1L) {
                    calls++
                    error("still failing")
                }
            }

            assertEquals(3, calls)
        }

    @Test
    fun `does not retry when shouldRetry says no`() =
        runTest {
            var calls = 0

            assertFailsWith<IllegalStateException> {
                withRetry(maxAttempts = 3, initialDelayMs = 1L, shouldRetry = { false }) {
                    calls++
                    error("not retryable")
                }
            }

            assertEquals(1, calls)
        }

    @Test
    fun `maxAttempts below 1 is rejected`() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                withRetry(maxAttempts = 0) { "unreachable" }
            }
        }

    @Test
    fun `defaultShouldRetry always allows another attempt`() {
        assertEquals(true, defaultShouldRetry(IllegalStateException()))
        assertEquals(true, defaultShouldRetry(RuntimeException("boom")))
    }
}
