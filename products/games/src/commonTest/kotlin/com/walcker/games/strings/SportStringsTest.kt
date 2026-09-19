package com.walcker.games.strings

import com.walcker.games.features.domain.shared.model.Sport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SportStringsTest {
    @Test
    fun `every sport has a name in both languages`() {
        for (sport in Sport.entries) {
            assertTrue(PtBrGamesStrings.sports.name(sport).isNotBlank(), "$sport sem nome em português")
            assertTrue(EnGamesStrings.sports.name(sport).isNotBlank(), "$sport sem nome em inglês")
        }
    }

    @Test
    fun `the portuguese names are the ones the app already showed`() {
        val shown =
            mapOf(
                Sport.FUTSAL to "Futsal",
                Sport.FUTEBOL to "Futebol",
                Sport.SOCIETY to "Society",
                Sport.VOLEI to "Vôlei",
                Sport.BASQUETE to "Basquete",
                Sport.BEACH_TENNIS to "Beach Tennis",
                Sport.TENIS to "Tênis",
                Sport.PADEL to "Padel",
                Sport.FUTEVOLEI to "Futevôlei",
                Sport.PICKLEBALL to "Pickleball",
                Sport.NATACAO to "Natação",
            )

        assertEquals(shown, Sport.entries.associateWith { PtBrGamesStrings.sports.name(it) })
    }

    @Test
    fun `english speakers read the sports in english`() {
        assertEquals("Soccer", EnGamesStrings.sports.name(Sport.FUTEBOL))
        assertEquals("Volleyball", EnGamesStrings.sports.name(Sport.VOLEI))
        assertEquals("Basketball", EnGamesStrings.sports.name(Sport.BASQUETE))
        assertEquals("Swimming", EnGamesStrings.sports.name(Sport.NATACAO))
    }
}
