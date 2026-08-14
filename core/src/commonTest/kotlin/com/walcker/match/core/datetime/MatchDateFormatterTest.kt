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
