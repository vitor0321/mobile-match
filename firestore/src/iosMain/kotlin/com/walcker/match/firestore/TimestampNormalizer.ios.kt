package com.walcker.match.firestore

internal actual fun normalizeTimestampMillis(value: Any?): Long? = normalizeNumericTimestamp(value)
