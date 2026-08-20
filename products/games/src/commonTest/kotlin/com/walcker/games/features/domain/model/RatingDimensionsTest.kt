package com.walcker.games.features.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RatingDimensionsTest {

    @Test
    fun `nasce vazio e sem nenhuma resposta`() {
        assertTrue(RatingDimensions.None.isEmpty)
        assertFalse(RatingDimensions.None.isComplete)
        assertNull(RatingDimensions.None[RatingDimension.PUNCTUALITY])
    }

    @Test
    fun `with grava a resposta da dimensao`() {
        val dimensions = RatingDimensions.None.with(RatingDimension.RESPECT, 4)

        assertEquals(4, dimensions[RatingDimension.RESPECT])
        assertFalse(dimensions.isEmpty)
        // Responder uma não inventa resposta para as outras.
        assertNull(dimensions[RatingDimension.PUNCTUALITY])
    }

    @Test
    fun `with nulo apaga a resposta`() {
        val answered = RatingDimensions.None.with(RatingDimension.FAIR_PLAY, 5)

        val cleared = answered.with(RatingDimension.FAIR_PLAY, null)

        assertTrue(cleared.isEmpty)
    }

    @Test
    fun `with sobrescreve sem duplicar`() {
        val dimensions = RatingDimensions.None
            .with(RatingDimension.BEHAVIOR, 2)
            .with(RatingDimension.BEHAVIOR, 5)

        assertEquals(mapOf(RatingDimension.BEHAVIOR to 5), dimensions.answers)
    }

    @Test
    fun `apagar dimensao que nunca foi respondida nao quebra`() {
        val dimensions = RatingDimensions.None.with(RatingDimension.RESPECT, null)

        assertTrue(dimensions.isEmpty)
    }

    @Test
    fun `recusa nota fora de 1 a 5 na construcao`() {
        for (invalid in listOf(0, 6, -1, 100)) {
            assertFailsWith<IllegalArgumentException> {
                RatingDimensions(mapOf(RatingDimension.PUNCTUALITY to invalid))
            }
        }
    }

    @Test
    fun `recusa nota fora da faixa vinda pelo with`() {
        assertFailsWith<IllegalArgumentException> {
            RatingDimensions.None.with(RatingDimension.PUNCTUALITY, 0)
        }
    }

    @Test
    fun `aceita as bordas da faixa`() {
        assertEquals(1, RatingDimensions.None.with(RatingDimension.RESPECT, 1)[RatingDimension.RESPECT])
        assertEquals(5, RatingDimensions.None.with(RatingDimension.RESPECT, 5)[RatingDimension.RESPECT])
    }

    /**
     * Estes nomes são contrato de rede: têm de bater com `RATING_DIMENSIONS` em
     * `functions/src/index.ts`. Se alguém renomear o enum achando que é só
     * cosmético, este teste é quem avisa.
     */
    @Test
    fun `nomes de campo batem com o contrato do servidor`() {
        assertEquals("punctuality", RatingDimension.PUNCTUALITY.wireName)
        assertEquals("respect", RatingDimension.RESPECT.wireName)
        assertEquals("fairPlay", RatingDimension.FAIR_PLAY.wireName)
        assertEquals("behavior", RatingDimension.BEHAVIOR.wireName)
    }

    @Test
    fun `campo agregado segue o padrao do perfil`() {
        assertEquals("fairPlayAverage", RatingDimension.FAIR_PLAY.averageField)
    }

    /**
     * O servidor recusa payload sem as quatro (`parseRatingDimensions` em
     * `functions/src/moderation.ts` trata ausente e inválido como o mesmo erro),
     * então a tela precisa saber dizer quando ainda não dá para enviar.
     */
    @Test
    fun `so fica completo com as quatro respondidas`() {
        var dimensions = RatingDimensions.None
        for ((index, dimension) in RatingDimension.entries.withIndex()) {
            assertFalse(dimensions.isComplete, "completo cedo demais com $index respostas")
            dimensions = dimensions.with(dimension, 4)
        }

        assertTrue(dimensions.isComplete)
    }

    @Test
    fun `apagar uma resposta desfaz o completo`() {
        val complete = RatingDimension.entries.fold(RatingDimensions.None) { acc, dimension ->
            acc.with(dimension, 3)
        }

        val incomplete = complete.with(RatingDimension.RESPECT, null)

        assertFalse(incomplete.isComplete)
    }

    @Test
    fun `as quatro dimensoes convivem`() {
        val dimensions = RatingDimension.entries.fold(RatingDimensions.None) { acc, dimension ->
            acc.with(dimension, 3)
        }

        assertEquals(4, dimensions.answers.size)
        assertTrue(dimensions.isComplete)
        assertTrue(RatingDimension.entries.all { dimensions[it] == 3 })
    }
}
