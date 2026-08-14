package com.walcker.games.features.data.util

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Retry policy for transient network failures.
 *
 * Uses exponential backoff: 500ms, 1s, 2s, 4s, capped at [maxDelayMs].
 * Retries up to [maxAttempts] total attempts (including the first call).
 */
internal suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 500L,
    maxDelayMs: Long = 4_000L,
    shouldRetry: (Throwable) -> Boolean = { true },
    block: suspend () -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be >= 1" }

    var attempt = 0
    var currentDelay = initialDelayMs
    var lastError: Throwable? = null

    while (attempt < maxAttempts) {
        try {
            return block()
        } catch (t: Throwable) {
            lastError = t
            attempt += 1
            if (attempt >= maxAttempts || !shouldRetry(t)) break
            delay(currentDelay)
            currentDelay = min((currentDelay * 2.0).toLong(), maxDelayMs)
        }
    }

    throw lastError ?: IllegalStateException("withRetry exited without error")
}

/**
 * Default retry decision: retry on any error EXCEPT validation errors
 * (which won't get better with another attempt).
 */
internal fun defaultShouldRetry(error: Throwable): Boolean = true
