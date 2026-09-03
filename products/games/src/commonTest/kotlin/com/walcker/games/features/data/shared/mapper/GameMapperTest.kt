package com.walcker.games.features.data.shared.mapper

import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.match.core.payments.formatCurrencyCents
import com.walcker.match.firestore.DocumentSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun snapshot(data: Map<String, Any?>): DocumentSnapshot =
    DocumentSnapshot(
        path = "matches/match-1",
        id = "match-1",
        data = data,
        exists = true,
    )

private fun fullMatchData(overrides: Map<String, Any?> = emptyMap()): Map<String, Any?> =
    mapOf(
        "sport" to "FUTSAL",
        "venueName" to "Quadra Central",
        "neighborhood" to "Centro",
        "city" to "São Paulo",
        "address" to "Rua Um, 100",
        "lat" to -23.55,
        "lng" to -46.63,
        "geohash" to "6gyf4",
        "startsAtSeconds" to 1_700_000_000L,
        "durationMin" to 90L,
        "confirmedCount" to 5L,
        "totalSlots" to 10L,
        "priceCents" to 2_500L,
        "organizerName" to "Organizador",
        "organizerId" to "organizer-1",
        "organizerRating" to 4.8,
        "status" to "OPEN",
        "participants" to listOf("p1", "p2"),
        "recurrence" to "WEEKLY",
        "seriesId" to "series-1",
    ) + overrides

class GameMapperTest {
    @Test
    fun `maps every field from a complete document`() {
        val game = snapshot(fullMatchData()).toGame()!!

        assertEquals("match-1", game.id)
        assertEquals(Sport.FUTSAL, game.sport)
        assertEquals("Quadra Central", game.venueName)
        assertEquals("Centro", game.neighborhood)
        assertEquals("São Paulo", game.city)
        assertEquals("Rua Um, 100", game.address)
        assertEquals(-23.55, game.lat)
        assertEquals(-46.63, game.lng)
        assertEquals("6gyf4", game.geohash)
        assertEquals(1_700_000_000L, game.startsAtSeconds)
        assertEquals(90, game.durationMin)
        assertEquals(5, game.confirmedPlayers)
        assertEquals(10, game.totalPlayers)
        assertEquals(formatCurrencyCents(2_500, "BRL"), game.pricePerPlayer)
        assertEquals(2_500, game.priceCents)
        assertEquals("BRL", game.currencyCode)
        assertEquals("Organizador", game.organizerName)
        assertEquals("organizer-1", game.organizerId)
        assertEquals(4.8, game.organizerRating)
        assertEquals(MatchStatus.OPEN, game.status)
        assertEquals(listOf("p1", "p2"), game.participants)
        assertEquals(RecurrenceOption.WEEKLY, game.recurrence)
        assertEquals("series-1", game.seriesId)
    }

    @Test
    fun `price is formatted using the organizer's currency, not a hardcoded one`() {
        val data = fullMatchData(mapOf("currencyCode" to "USD"))

        val game = snapshot(data).toGame()!!

        assertEquals("USD", game.currencyCode)
        assertEquals(formatCurrencyCents(2_500, "USD"), game.pricePerPlayer)
    }

    @Test
    fun `missing required field returns null instead of throwing`() {
        val data = fullMatchData() - "venueName"

        assertNull(snapshot(data).toGame())
    }

    @Test
    fun `unrecognized sport returns null`() {
        val data = fullMatchData(mapOf("sport" to "CHESS"))

        assertNull(snapshot(data).toGame())
    }

    @Test
    fun `unrecognized status falls back to OPEN`() {
        val data = fullMatchData(mapOf("status" to "banana"))

        assertEquals(MatchStatus.OPEN, snapshot(data).toGame()!!.status)
    }

    @Test
    fun `missing optional fields fall back to their defaults`() {
        val data =
            fullMatchData() -
                setOf(
                    "durationMin",
                    "confirmedCount",
                    "totalSlots",
                    "priceCents",
                    "organizerRating",
                    "status",
                    "participants",
                    "recurrence",
                    "seriesId",
                )

        val game = snapshot(data).toGame()!!

        assertEquals(60, game.durationMin)
        assertEquals(0, game.confirmedPlayers)
        assertEquals(1, game.totalPlayers)
        assertNull(game.pricePerPlayer)
        assertEquals(0, game.priceCents)
        assertEquals(0.0, game.organizerRating)
        assertEquals(0, game.organizerRatingCount)
        assertEquals(0.0, game.matchRating)
        assertEquals(0, game.matchRatingCount)
        assertEquals(MatchStatus.OPEN, game.status)
        assertEquals(emptyList(), game.participants)
        assertEquals(RecurrenceOption.NONE, game.recurrence)
        assertNull(game.seriesId)
    }

    @Test
    fun `a zero price is treated as free, not R dollar 0,00`() {
        val data = fullMatchData(mapOf("priceCents" to 0L))

        assertNull(snapshot(data).toGame()!!.pricePerPlayer)
    }

    @Test
    fun `reads startsAtSeconds from the legacy startsAt long field`() {
        val data = (fullMatchData() - "startsAtSeconds") + mapOf("startsAt" to 1_650_000_000L)

        assertEquals(1_650_000_000L, snapshot(data).toGame()!!.startsAtSeconds)
    }

    @Test
    fun `reads startsAtSeconds from a Firestore timestamp map`() {
        val data =
            (fullMatchData() - "startsAtSeconds") +
                mapOf("startsAt" to mapOf("seconds" to 1_660_000_000L, "nanoseconds" to 0L))

        assertEquals(1_660_000_000L, snapshot(data).toGame()!!.startsAtSeconds)
    }

    @Test
    fun `missing every startsAt variant returns null`() {
        val data = fullMatchData() - "startsAtSeconds"

        assertNull(snapshot(data).toGame())
    }

    @Test
    fun `non-string participant entries are dropped`() {
        val data = fullMatchData(mapOf("participants" to listOf("p1", 42, null, "p2")))

        assertEquals(listOf("p1", "p2"), snapshot(data).toGame()!!.participants)
    }
}
