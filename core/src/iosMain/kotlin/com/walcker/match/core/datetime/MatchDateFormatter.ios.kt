package com.walcker.match.core.datetime

import kotlinx.datetime.Instant
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun getCurrentTime(): Instant {
    val now = NSDate()
    val seconds = now.timeIntervalSince1970.toLong()
    return Instant.fromEpochSeconds(seconds)
}
