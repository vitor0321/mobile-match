package com.walcker.match.core.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsEventTest {

    @Test
    fun `os nomes do funil sao os esperados`() {
        assertEquals("match_list_viewed", AnalyticsEvent.MatchListViewed(MatchListSource.HOME).name)
        assertEquals("match_viewed", AnalyticsEvent.MatchViewed("FUTSAL", true).name)
        assertEquals("match_join_attempted", AnalyticsEvent.MatchJoinAttempted("FUTSAL").name)
        assertEquals(
            "match_join_result",
            AnalyticsEvent.MatchJoinResult("FUTSAL", JoinOutcome.CONFIRMED).name,
        )
        assertEquals("match_left", AnalyticsEvent.MatchLeft("FUTSAL").name)
        assertEquals("match_created", AnalyticsEvent.MatchCreated("FUTSAL").name)
        assertEquals("player_rated", AnalyticsEvent.PlayerRated(4).name)
    }

    @Test
    fun `nenhum nome estoura o limite do Firebase`() {
        val events = listOf(
            AnalyticsEvent.MatchListViewed(MatchListSource.HOME),
            AnalyticsEvent.MatchViewed("FUTSAL", true),
            AnalyticsEvent.MatchJoinAttempted("FUTSAL"),
            AnalyticsEvent.MatchJoinResult("FUTSAL", JoinOutcome.WAITLIST),
            AnalyticsEvent.MatchLeft("FUTSAL"),
            AnalyticsEvent.MatchCreated("FUTSAL"),
            AnalyticsEvent.PlayerRated(5),
        )

        events.forEach { event ->
            assertTrue(event.name.length <= 40, "nome longo demais: ${event.name}")
            assertTrue(
                event.name.all { it.isLowerCase() || it == '_' },
                "fora do snake_case: ${event.name}",
            )
            event.params.keys.forEach { key ->
                assertTrue(key.length <= 40, "parâmetro longo demais: $key")
            }
        }
    }

    @Test
    fun `nenhum evento carrega id`() {
        val events = listOf(
            AnalyticsEvent.MatchViewed("FUTSAL", true),
            AnalyticsEvent.MatchJoinResult("FUTSAL", JoinOutcome.CONFIRMED),
            AnalyticsEvent.PlayerRated(3),
        )

        events.forEach { event ->
            assertTrue(
                event.params.keys.none { it.contains("id", ignoreCase = true) },
                "evento ${event.name} carrega id: ${event.params.keys}",
            )
        }
    }

    @Test
    fun `o desfecho de entrar vira dimensao, nao evento separado`() {
        val confirmado = AnalyticsEvent.MatchJoinResult("VOLEI", JoinOutcome.CONFIRMED)
        val fila = AnalyticsEvent.MatchJoinResult("VOLEI", JoinOutcome.WAITLIST)
        val falhou = AnalyticsEvent.MatchJoinResult("VOLEI", JoinOutcome.FAILED)

        assertEquals(confirmado.name, fila.name)
        assertEquals(confirmado.name, falhou.name)
        assertEquals("confirmed", confirmado.params["outcome"])
        assertEquals("waitlist", fila.params["outcome"])
        assertEquals("failed", falhou.params["outcome"])
    }

    @Test
    fun `os parametros de segmentacao chegam inteiros`() {
        val visto = AnalyticsEvent.MatchViewed(sport = "BEACH_TENNIS", hasOpenSlots = false)

        assertEquals(mapOf("sport" to "BEACH_TENNIS", "has_open_slots" to "false"), visto.params)
        assertEquals(mapOf("stars" to "4"), AnalyticsEvent.PlayerRated(4).params)
        assertEquals(
            mapOf("source" to "map"),
            AnalyticsEvent.MatchListViewed(MatchListSource.MAP).params,
        )
    }
}
