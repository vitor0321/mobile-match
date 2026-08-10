package com.walcker.identity.features.data.pro

internal expect fun currentDayKey(): String
internal expect fun formatEpochMillisToDayKey(epochMillis: Long): String
internal expect fun daysBetween(startDayKey: String, endDayKey: String): Int
