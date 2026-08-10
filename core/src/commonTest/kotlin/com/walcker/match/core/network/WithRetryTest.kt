package com.walcker.match.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class WithRetryTest {
    @Test
    fun `When block succeeds should execute only once`() = runTest {
        var attempts = 0

        val result = withRetry {
            attempts += 1
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `When transient failure recovers should retry with configured delay`() = runTest {
        var attempts = 0

        val result = withRetry(retries = 3, initialDelay = 100) {
            attempts += 1
            if (attempts < 3) throw RetryableException()
            "success"
        }

        assertEquals("success", result)
        assertEquals(3, attempts)
        assertEquals(300, testScheduler.currentTime)
    }

    @Test
    fun `When retries are exhausted should propagate final transient failure`() = runTest {
        var attempts = 0

        assertFailsWith<RetryableException> {
            withRetry(retries = 3, initialDelay = 100) {
                attempts += 1
                throw RetryableException()
            }
        }

        assertEquals(3, attempts)
        assertEquals(300, testScheduler.currentTime)
    }

    @Test
    fun `When failure is not retryable should propagate without delay`() = runTest {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            withRetry(retries = 3) {
                attempts += 1
                throw IllegalStateException("permanent")
            }
        }

        assertEquals(1, attempts)
        assertEquals(0, testScheduler.currentTime)
    }

    @Test
    fun `When block is cancelled should propagate cancellation without retry`() = runTest {
        var attempts = 0

        assertFailsWith<CancellationException> {
            withRetry {
                attempts += 1
                throw CancellationException("cancelled")
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `When cancelled during backoff should not execute another attempt`() = runTest {
        var attempts = 0
        val retryJob: Job = launch {
            withRetry(initialDelay = 100) {
                attempts += 1
                throw RetryableException()
            }
        }

        runCurrent()
        retryJob.cancelAndJoin()
        advanceTimeBy(10_000)

        assertEquals(1, attempts)
    }
}
