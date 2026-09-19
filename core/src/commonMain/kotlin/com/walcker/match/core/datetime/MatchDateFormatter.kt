package com.walcker.match.core.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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

public fun formatWhen(
    startsAtSeconds: Long,
    now: Instant = getCurrentTime(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String =
    formatWhen(
        starts = Instant.fromEpochSeconds(startsAtSeconds),
        now = now,
        timeZone = timeZone,
    )

public fun formatDayLabel(
    starts: Instant,
    now: Instant = getCurrentTime(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val startLocal = starts.toLocalDateTime(timeZone)
    val nowLocal = now.toLocalDateTime(timeZone)
    val tomorrowLocal = now.plus(1, DateTimeUnit.DAY, timeZone).toLocalDateTime(timeZone)

    return when {
        startLocal.isSameDayAs(nowLocal) -> "Hoje"
        startLocal.isSameDayAs(tomorrowLocal) -> "Amanhã"
        else -> "${startLocal.dayOfMonth.pad2()}/${startLocal.monthNumber.pad2()}"
    }
}

public fun formatDayLabel(
    startsAtSeconds: Long,
    now: Instant = getCurrentTime(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String =
    formatDayLabel(
        starts = Instant.fromEpochSeconds(startsAtSeconds),
        now = now,
        timeZone = timeZone,
    )

public fun formatTimeRange(
    startsAtSeconds: Long,
    durationMin: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val start = Instant.fromEpochSeconds(startsAtSeconds)
    val end = start.plus(durationMin.toLong(), DateTimeUnit.MINUTE, timeZone)
    val startLocal = start.toLocalDateTime(timeZone)
    val endLocal = end.toLocalDateTime(timeZone)
    return "${startLocal.hour.pad2()}:${startLocal.minute.pad2()} - ${endLocal.hour.pad2()}:${endLocal.minute.pad2()}"
}

internal expect fun getCurrentTime(): Instant

private fun LocalDateTime.isSameDayAs(other: LocalDateTime): Boolean = year == other.year && monthNumber == other.monthNumber && dayOfMonth == other.dayOfMonth

private fun Int.pad2(): String = if (this < 10) "0$this" else toString()

public fun formatShortDate(
    instant: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = instant.toLocalDateTime(timeZone)
    return "${local.dayOfMonth.pad2()}/${local.monthNumber.pad2()}/${local.year}"
}

public fun formatShortDate(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String =
    if (epochMillis <= 0L) {
        ""
    } else {
        formatShortDate(Instant.fromEpochMilliseconds(epochMillis), timeZone)
    }
