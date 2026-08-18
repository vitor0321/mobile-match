package com.walcker.match.core.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * pt-BR relative label for when a match starts.
 *
 * Ports `formatWhen` from `lib/geo.ts` in the Lovable MVP: "Hoje · HH:MM" when
 * the match is today, "Amanhã · HH:MM" when tomorrow, "DD/MM · HH:MM" otherwise.
 */
public fun formatWhen(
    starts: Instant,
    now: Instant = getCurrentTime(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val startLocal = starts.toLocalDateTime(timeZone)
    val nowLocal = now.toLocalDateTime(timeZone)
    val tomorrowLocal = now.plus(1, DateTimeUnit.DAY, timeZone).toLocalDateTime(timeZone)

    val time = "${startLocal.hour.pad2()}:${startLocal.minute.pad2()}"

    return when {
        startLocal.isSameDayAs(nowLocal) -> "Hoje · $time"
        startLocal.isSameDayAs(tomorrowLocal) -> "Amanhã · $time"
        else -> "${startLocal.dayOfMonth.pad2()}/${startLocal.monthNumber.pad2()} · $time"
    }
}

/**
 * Convenience overload that takes a Firestore-style epoch-seconds Long.
 */
public fun formatWhen(
    startsAtSeconds: Long,
    now: Instant = getCurrentTime(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String = formatWhen(
    starts = Instant.fromEpochSeconds(startsAtSeconds),
    now = now,
    timeZone = timeZone,
)

/**
 * Platform-agnostic way to get the current time.
 * Uses expect/actual for platform-specific implementation.
 */
internal expect fun getCurrentTime(): Instant

private fun LocalDateTime.isSameDayAs(other: LocalDateTime): Boolean =
    year == other.year && monthNumber == other.monthNumber && dayOfMonth == other.dayOfMonth

private fun Int.pad2(): String = if (this < 10) "0$this" else toString()

/**
 * Locale-neutral numeric date: `18/08/2026`.
 *
 * Used where a full relative label ([formatWhen]) would be noise — rating cards,
 * "member since", history lists. Numeric on purpose: month names would need a
 * translation table in `core`, which has no strings layer.
 */
public fun formatShortDate(
    instant: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = instant.toLocalDateTime(timeZone)
    return "${local.dayOfMonth.pad2()}/${local.monthNumber.pad2()}/${local.year}"
}

/**
 * Convenience overload for epoch-millis values (how Firestore rating documents
 * store `createdAtMs`). Returns an empty string for a missing/zero timestamp so
 * the UI can skip rendering instead of showing `01/01/1970`.
 */
public fun formatShortDate(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String = if (epochMillis <= 0L) {
    ""
} else {
    formatShortDate(Instant.fromEpochMilliseconds(epochMillis), timeZone)
}
