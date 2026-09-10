@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.walcker.match.firestore

import platform.Foundation.NSDate
import platform.Foundation.NSNumber
import platform.Foundation.NSSelectorFromString
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.valueForKey
import platform.darwin.NSObject

private const val MILLIS_PER_SECOND = 1_000L
private const val NANOS_PER_MILLI = 1_000_000L

internal actual fun normalizeTimestampMillis(value: Any?): Long? =
    when (value) {
        null -> null
        is NSDate -> (value.timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()
        is NSObject -> value.asFirebaseTimestampMillis() ?: normalizeNumericTimestamp(value)
        else -> normalizeNumericTimestamp(value)
    }

private fun NSObject.asFirebaseTimestampMillis(): Long? {
    val secondsSelector = NSSelectorFromString("seconds")
    val nanosecondsSelector = NSSelectorFromString("nanoseconds")
    if (!respondsToSelector(secondsSelector) || !respondsToSelector(nanosecondsSelector)) return null

    val seconds = (valueForKey("seconds") as? NSNumber)?.longLongValue ?: return null
    val nanoseconds = (valueForKey("nanoseconds") as? NSNumber)?.intValue ?: 0
    return seconds * MILLIS_PER_SECOND + nanoseconds / NANOS_PER_MILLI
}
