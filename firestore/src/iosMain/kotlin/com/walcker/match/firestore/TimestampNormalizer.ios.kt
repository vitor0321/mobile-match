package com.walcker.match.firestore

/**
 * The iOS Firestore client is still a stub (see `FirestoreClient.ios.kt`), so no
 * Objective-C timestamp type ever reaches this function yet. Numeric fields —
 * the convention new collections follow — are handled by the shared helper.
 */
internal actual fun normalizeTimestampMillis(value: Any?): Long? =
    normalizeNumericTimestamp(value)
