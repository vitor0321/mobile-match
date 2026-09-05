package com.walcker.match.firestore

import com.google.firebase.Timestamp
import java.util.Date

internal actual fun normalizeTimestampMillis(value: Any?): Long? =
    when (value) {
        null -> null
        is Timestamp -> value.toDate().time
        is Date -> value.time
        else -> normalizeNumericTimestamp(value)
    }
