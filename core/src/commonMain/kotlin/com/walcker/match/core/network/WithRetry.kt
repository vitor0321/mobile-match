package com.walcker.match.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_RETRIES = 3
private const val INITIAL_DELAY_MS = 500L
private const val MAX_DELAY_MS = 10_000L
private const val BACKOFF_FACTOR = 2

open class RetryableException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

suspend fun <T> withRetry(
    retries: Int = DEFAULT_RETRIES,
    initialDelay: Long = INITIAL_DELAY_MS,
    maxDelay: Long = MAX_DELAY_MS,
    shouldRetry: (Throwable) -> Boolean = { it is RetryableException },
    block: suspend () -> T,
): T {
    require(retries > 0) { "retries must be greater than zero" }
    require(initialDelay >= 0) { "initialDelay must not be negative" }
    require(maxDelay >= initialDelay) { "maxDelay must be greater than or equal to initialDelay" }

    var currentDelay = initialDelay
    repeat(retries - 1) {
        try {
            return block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            if (!shouldRetry(error)) throw error
        }

        delay(currentDelay.milliseconds)
        currentDelay = (currentDelay * BACKOFF_FACTOR).coerceAtMost(maxDelay)
    }

    return block()
}
