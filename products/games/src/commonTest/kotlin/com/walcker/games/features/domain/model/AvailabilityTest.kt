package com.walcker.games.features.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Esta regra tem de ser byte a byte a mesma do `isAvailableAt` em
 * `functions/src/notifications.ts`. Se as duas divergirem, o app mostra
 * "disponível" para alguém que o servidor não está notificando — e ninguém
 * descobre, porque o sintoma é silêncio.
 */
class AvailabilityTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `quem nunca tocou no toggle esta indisponivel`() {
        assertFalse(Availability.Unavailable.isAvailable)
        assertNull(Availability.Unavailable.availableUntilMs)
        assertFalse(Availability.Unavailable.isActiveAt(now))
    }

    @Test
    fun `disponivel sem vencimento vale sempre`() {
        // Null é "até eu desligar", que é o que o toggle grava hoje.
        val semVencimento = Availability(isAvailable = true, availableUntilMs = null)

        assertTrue(semVencimento.isActiveAt(now))
        assertTrue(semVencimento.isActiveAt(now + 365L * 24 * 60 * 60 * 1000))
    }

    @Test
    fun `toggle desligado vence qualquer janela aberta`() {
        val desligado = Availability(isAvailable = false, availableUntilMs = now + 10_000)

        assertFalse(desligado.isActiveAt(now))
    }

    @Test
    fun `janela vencida vale como indisponivel`() {
        // Vencer sozinho é o que evita ter de varrer a base desligando campo.
        val vencido = Availability(isAvailable = true, availableUntilMs = now - 1)

        assertFalse(vencido.isActiveAt(now))
    }

    @Test
    fun `a janela vale ate o ultimo instante`() {
        val aberta = Availability(isAvailable = true, availableUntilMs = now + 1)
        val fechada = Availability(isAvailable = true, availableUntilMs = now)

        assertTrue(aberta.isActiveAt(now))
        // No instante exato do vencimento já está fora — igual ao servidor,
        // que compara com `> nowMs`.
        assertFalse(fechada.isActiveAt(now))
    }
}
