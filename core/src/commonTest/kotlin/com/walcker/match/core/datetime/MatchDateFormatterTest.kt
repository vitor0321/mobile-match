package com.walcker.match.core.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDateFormatterTest {

    private val utc = TimeZone.UTC
    private val saoPaulo = TimeZone.of("America/Sao_Paulo")

    @Test
    fun `formatWhen today in UTC`() {
        val now = Instant.parse("2026-08-12T15:30:00Z")
        val matchTime = now.plus(4, DateTimeUnit.HOUR, utc) // 19:30 UTC, mesmo dia
        assertEquals("Hoje · 19:30", formatWhen(matchTime, now, utc))
    }

    @Test
    fun `formatWhen today in São Paulo`() {
        val matchSp = LocalDateTime(2026, 8, 12, 19, 30).toInstant(saoPaulo)
        val nowSp = LocalDateTime(2026, 8, 12, 15, 30).toInstant(saoPaulo)
        assertEquals("Hoje · 19:30", formatWhen(matchSp, nowSp, saoPaulo))
    }

    @Test
    fun `formatWhen tomorrow in São Paulo`() {
        val nowSp = LocalDateTime(2026, 8, 12, 20, 0).toInstant(saoPaulo)
        val matchSp = LocalDateTime(2026, 8, 13, 19, 30).toInstant(saoPaulo)
        assertEquals("Amanhã · 19:30", formatWhen(matchSp, nowSp, saoPaulo))
    }

    @Test
    fun `formatWhen another day`() {
        val nowSp = LocalDateTime(2026, 8, 12, 20, 0).toInstant(saoPaulo)
        val matchSp = LocalDateTime(2026, 8, 15, 19, 30).toInstant(saoPaulo)
        assertEquals("15/08 · 19:30", formatWhen(matchSp, nowSp, saoPaulo))
    }
}

class FormatShortDateTest {

    private val saoPaulo = TimeZone.of("America/Sao_Paulo")

    @Test
    fun `formats an instant as a zero padded numeric date`() {
        val instant = Instant.parse("2026-08-18T12:00:00Z")

        assertEquals("18/08/2026", formatShortDate(instant, saoPaulo))
    }

    @Test
    fun `respects the requested time zone when the day differs`() {
        // 01:00 UTC on the 19th is still the 18th in Sao Paulo (UTC-3)
        val instant = Instant.parse("2026-08-19T01:00:00Z")

        assertEquals("18/08/2026", formatShortDate(instant, saoPaulo))
        assertEquals("19/08/2026", formatShortDate(instant, TimeZone.UTC))
    }

    @Test
    fun `returns an empty string for a missing timestamp`() {
        assertEquals("", formatShortDate(epochMillis = 0L, timeZone = saoPaulo))
        assertEquals("", formatShortDate(epochMillis = -1L, timeZone = saoPaulo))
    }

    @Test
    fun `formats epoch millis`() {
        val millis = Instant.parse("2026-01-05T15:00:00Z").toEpochMilliseconds()

        assertEquals("05/01/2026", formatShortDate(epochMillis = millis, timeZone = saoPaulo))
    }
}
