package com.walcker.games.features.ui.home.map

import com.walcker.games.fake.game
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.home.map.mapper.toMapPin
import com.walcker.games.strings.EnGamesStrings
import com.walcker.games.strings.PtBrGamesStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class MapPinTest {
    private val volleyball = game(id = "m1").copy(sport = Sport.VOLEI, venueName = "Arena")

    @Test
    fun `the pin title names the sport in the app language`() {
        assertEquals("Vôlei · Arena", volleyball.toMapPin(PtBrGamesStrings.sports, PtBrGamesStrings.map.freePrice).title)
        assertEquals("Volleyball · Arena", volleyball.toMapPin(EnGamesStrings.sports, EnGamesStrings.map.freePrice).title)
    }

    @Test
    fun `the pin keeps the match id and position`() {
        val pin = volleyball.toMapPin(PtBrGamesStrings.sports, PtBrGamesStrings.map.freePrice)

        assertEquals("m1", pin.matchId)
        assertEquals(volleyball.lat, pin.lat)
        assertEquals(volleyball.lng, pin.lng)
    }

    @Test
    fun `a free match says so in the app language`() {
        val free = volleyball.copy(pricePerPlayer = null)

        assertEquals(true, free.toMapPin(EnGamesStrings.sports, EnGamesStrings.map.freePrice).snippet.endsWith("Free"))
        assertEquals(true, free.toMapPin(PtBrGamesStrings.sports, PtBrGamesStrings.map.freePrice).snippet.endsWith("Grátis"))
    }
}
