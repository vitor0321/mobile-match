package com.walcker.games.features.domain.shared.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        val vencido = Availability(isAvailable = true, availableUntilMs = now - 1)

        assertFalse(vencido.isActiveAt(now))
    }

    @Test
    fun `a janela vale ate o ultimo instante`() {
        val aberta = Availability(isAvailable = true, availableUntilMs = now + 1)
        val fechada = Availability(isAvailable = true, availableUntilMs = now)

        assertTrue(aberta.isActiveAt(now))
        assertFalse(fechada.isActiveAt(now))
    }
}
