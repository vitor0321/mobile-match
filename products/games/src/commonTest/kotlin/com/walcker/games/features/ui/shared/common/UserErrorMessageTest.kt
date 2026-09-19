package com.walcker.games.features.ui.shared.common

import com.walcker.games.features.domain.shared.error.GamesError
import com.walcker.games.strings.EnGamesStrings
import com.walcker.games.strings.PtBrGamesStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class UserErrorMessageTest {
    private val errors = PtBrGamesStrings.errors

    @Test
    fun `a connection problem gets the no connection message`() {
        assertEquals(errors.noConnection, GamesError.Network().userMessage(fallback = "genérico", errors = errors))
    }

    @Test
    fun `any other problem gets the screen message`() {
        assertEquals("genérico", GamesError.PermissionDenied().userMessage(fallback = "genérico", errors = errors))
        assertEquals("genérico", GamesError.Unknown().userMessage(fallback = "genérico", errors = errors))
    }

    @Test
    fun `the technical message of an exception never reaches the user`() {
        val technical = IllegalStateException("PERMISSION_DENIED: Missing or insufficient permissions.")

        assertEquals("genérico", technical.userMessage(fallback = "genérico", errors = errors))
    }

    @Test
    fun `the no connection message exists in both languages`() {
        assertEquals("Sem conexão. Confira sua internet e tente de novo.", PtBrGamesStrings.errors.noConnection)
        assertEquals("No connection. Check your internet and try again.", EnGamesStrings.errors.noConnection)
    }
}
