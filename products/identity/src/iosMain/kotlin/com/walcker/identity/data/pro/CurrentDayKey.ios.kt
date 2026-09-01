package com.walcker.identity.features.data.pro

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSinceDate

internal actual fun currentDayKey(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(NSDate())
}

internal actual fun formatEpochMillisToDayKey(epochMillis: Long): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

internal actual fun daysBetween(
    startDayKey: String,
    endDayKey: String,
): Int {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    val date1 = formatter.dateFromString(startDayKey) ?: return 0
    val date2 = formatter.dateFromString(endDayKey) ?: return 0
    val diff = date2.timeIntervalSinceDate(date1)
    return (diff / (60 * 60 * 24)).toInt()
}
