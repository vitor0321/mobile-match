package com.walcker.match.firestore

/**
 * Converts whatever a platform Firestore SDK put in a document field into epoch
 * milliseconds.
 *
 * Firestore returns `Timestamp`/`Date` objects for timestamp fields, so a plain
 * `data[field] as? Long` silently yields `null` and every date renders as
 * 01/01/1970. Each platform unwraps its own SDK type here; numeric fields
 * (our preferred `*Ms` convention) pass through unchanged.
 *
 * @return epoch millis, or `null` when the field is absent or not a time value
 */
internal expect fun normalizeTimestampMillis(value: Any?): Long?

/**
 * Shared portion of the conversion: everything that is already a number.
 * Platform actuals delegate here after handling their own SDK types.
 */
internal fun normalizeNumericTimestamp(value: Any?): Long? = when (value) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.toLong()
    is Float -> value.toLong()
    else -> null
}
