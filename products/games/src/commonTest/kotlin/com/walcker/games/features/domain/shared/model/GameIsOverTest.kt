package com.walcker.games.features.domain.shared.model

import com.walcker.games.fake.game
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameIsOverTest {
    private val start = 1_000_000L
    private val sixtyMin = game(startsAtSeconds = start, durationMin = 60)
    private val end = start + 60 * 60

    @Test
    fun `fim e o inicio mais a duracao`() {
        assertEquals(end, sixtyMin.endsAtSeconds)
    }

    @Test
    fun `nao acabou antes de comecar`() {
        assertFalse(sixtyMin.isOver(nowSeconds = start - 1))
    }

    @Test
    fun `nao acabou durante a partida`() {
        assertFalse(sixtyMin.isOver(nowSeconds = start + 30 * 60))
    }

    @Test
    fun `nao acabou um segundo antes do fim`() {
        assertFalse(sixtyMin.isOver(nowSeconds = end - 1))
    }

    @Test
    fun `acabou exatamente no fim`() {
        assertTrue(sixtyMin.isOver(nowSeconds = end))
    }

    @Test
    fun `acabou depois do fim`() {
        assertTrue(sixtyMin.isOver(nowSeconds = end + 1))
    }

    @Test
    fun `duracao negativa conta como zero, igual ao servidor`() {
        val malformed = game(startsAtSeconds = start, durationMin = -30)

        assertEquals(start, malformed.endsAtSeconds)
        assertTrue(malformed.isOver(nowSeconds = start))
        assertFalse(malformed.isOver(nowSeconds = start - 1))
    }

    @Test
    fun `duracao zero encerra no horario de inicio`() {
        val instant = game(startsAtSeconds = start, durationMin = 0)

        assertTrue(instant.isOver(nowSeconds = start))
        assertFalse(instant.isOver(nowSeconds = start - 1))
    }
}

class GameCanBeRatedByParticipantTest {
    private val start = 1_000_000L
    private val end = start + 60 * 60
    private val player = "player-1"

    private fun match(
        status: MatchStatus = MatchStatus.OPEN,
        participants: List<String> = listOf(player, "player-2"),
    ) = game(
        startsAtSeconds = start,
        durationMin = 60,
        status = status,
        participants = participants,
    )

    @Test
    fun `libera para quem jogou depois que a partida acabou`() {
        assertTrue(match().canBeRatedByParticipant(userId = player, nowSeconds = end))
    }

    @Test
    fun `nao libera enquanto a partida nao acabou`() {
        assertFalse(match().canBeRatedByParticipant(userId = player, nowSeconds = end - 1))
    }

    @Test
    fun `nao libera para quem nao jogou`() {
        assertFalse(match().canBeRatedByParticipant(userId = "estranho", nowSeconds = end))
    }

    @Test
    fun `nao libera sem sessao resolvida`() {
        assertFalse(match().canBeRatedByParticipant(userId = null, nowSeconds = end))
    }

    @Test
    fun `nao libera em partida cancelada`() {
        val cancelled = match(status = MatchStatus.CANCELLED)

        assertFalse(cancelled.canBeRatedByParticipant(userId = player, nowSeconds = end))
    }

    @Test
    fun `status OPEN nao impede avaliar - e o caso normal`() {
        val stillOpen = match(status = MatchStatus.OPEN)

        assertTrue(stillOpen.canBeRatedByParticipant(userId = player, nowSeconds = end))
    }

    @Test
    fun `status FULL tambem nao impede`() {
        assertTrue(match(status = MatchStatus.FULL).canBeRatedByParticipant(player, end))
    }
}

class GameCanOrganizerRateTest {
    private val organizer = "organizer-1"

    private fun match(
        status: MatchStatus = MatchStatus.OPEN,
        organizerId: String = organizer,
    ) = game(status = status, organizerId = organizerId)

    @Test
    fun `libera para o organizador`() {
        assertTrue(match().canOrganizerRate(userId = organizer))
    }

    @Test
    fun `nao libera para quem nao e organizador`() {
        assertFalse(match().canOrganizerRate(userId = "outro-jogador"))
    }

    @Test
    fun `nao libera sem sessao resolvida`() {
        assertFalse(match().canOrganizerRate(userId = null))
    }

    @Test
    fun `nao libera em partida cancelada, mesmo para o organizador`() {
        val cancelled = match(status = MatchStatus.CANCELLED)

        assertFalse(cancelled.canOrganizerRate(userId = organizer))
    }

    @Test
    fun `libera antes da partida comecar - nao ha trava de tempo`() {
        val future = game(startsAtSeconds = 999_999_999L, organizerId = organizer)

        assertTrue(future.canOrganizerRate(userId = organizer))
    }
}

class GameCanRateOrganizerTest {
    private val organizer = "organizer-1"
    private val participant = "player-2"

    private fun match(
        status: MatchStatus = MatchStatus.OPEN,
        participants: List<String> = listOf(organizer, participant),
    ) = game(status = status, organizerId = organizer, participants = participants)

    @Test
    fun `libera para participante confirmado que nao e o organizador`() {
        assertTrue(match().canRateOrganizer(userId = participant))
    }

    @Test
    fun `nao libera para o proprio organizador`() {
        assertFalse(match().canRateOrganizer(userId = organizer))
    }

    @Test
    fun `nao libera para quem nao esta confirmado na partida`() {
        assertFalse(match().canRateOrganizer(userId = "estranho"))
    }

    @Test
    fun `nao libera sem sessao resolvida`() {
        assertFalse(match().canRateOrganizer(userId = null))
    }

    @Test
    fun `nao libera em partida cancelada`() {
        val cancelled = match(status = MatchStatus.CANCELLED)

        assertFalse(cancelled.canRateOrganizer(userId = participant))
    }
}
