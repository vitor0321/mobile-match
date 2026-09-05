package com.walcker.identity.features.data.pro

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual fun currentDayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

internal actual fun formatEpochMillisToDayKey(epochMillis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))

internal actual fun daysBetween(
    startDayKey: String,
    endDayKey: String,
): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date1 = sdf.parse(startDayKey) ?: return 0
        val date2 = sdf.parse(endDayKey) ?: return 0
        val diff = date2.time - date1.time
        (diff / (1000 * 60 * 60 * 24)).toInt()
    } catch (e: Exception) {
        0
    }
}
