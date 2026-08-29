package com.walcker.match.firestore

internal expect fun normalizeTimestampMillis(value: Any?): Long?

internal fun normalizeNumericTimestamp(value: Any?): Long? = when (value) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.toLong()
    is Float -> value.toLong()
    else -> null
}
