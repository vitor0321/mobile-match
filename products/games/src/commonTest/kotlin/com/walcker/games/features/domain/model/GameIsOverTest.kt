package com.walcker.games.features.domain.model

import com.walcker.games.fake.game
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A borda aqui não é cosmética: é o mesmo instante em que `requireMatchIsOver`
 * das Functions passa a aceitar `submitPlayerRating`. Se o cliente e o servidor
 * discordarem em um segundo, a tela oferece um botão que a Function recusa.
 */
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
        // O servidor recusa quando `endsAtMillis > Date.now()`, ou seja: no
        // instante exato do fim ele já aceita. O cliente tem de aceitar junto.
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

class GameCanBeRatedByTest {

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
        assertTrue(match().canBeRatedBy(userId = player, nowSeconds = end))
    }

    @Test
    fun `nao libera enquanto a partida nao acabou`() {
        assertFalse(match().canBeRatedBy(userId = player, nowSeconds = end - 1))
    }

    @Test
    fun `nao libera para quem nao jogou`() {
        assertFalse(match().canBeRatedBy(userId = "estranho", nowSeconds = end))
    }

    @Test
    fun `nao libera sem sessao resolvida`() {
        assertFalse(match().canBeRatedBy(userId = null, nowSeconds = end))
    }

    @Test
    fun `nao libera em partida cancelada`() {
        val cancelled = match(status = MatchStatus.CANCELLED)

        assertFalse(cancelled.canBeRatedBy(userId = player, nowSeconds = end))
    }

    @Test
    fun `status OPEN nao impede avaliar - e o caso normal`() {
        // Nada escreve FINISHED. Uma partida encerrada segue com status OPEN, e
        // era exatamente isso que deixava o botão invisível para sempre.
        val stillOpen = match(status = MatchStatus.OPEN)

        assertTrue(stillOpen.canBeRatedBy(userId = player, nowSeconds = end))
    }

    @Test
    fun `status FULL tambem nao impede`() {
        assertTrue(match(status = MatchStatus.FULL).canBeRatedBy(player, end))
    }
}
