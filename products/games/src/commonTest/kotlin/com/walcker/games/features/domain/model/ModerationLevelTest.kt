package com.walcker.games.features.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ModerationLevelTest {

    @Test
    fun `ids match the functions contract`() {
        assertEquals(
            listOf("none", "warning", "suspended", "banned"),
            ModerationLevel.entries.map { it.id },
        )
    }

    @Test
    fun `an unknown or missing level reads as unrestricted`() {
        assertEquals(ModerationLevel.NONE, ModerationLevel.fromId(null))
        assertEquals(ModerationLevel.NONE, ModerationLevel.fromId("shadowbanned"))
    }

    @Test
    fun `fromId round-trips every level`() {
        ModerationLevel.entries.forEach { level ->
            assertEquals(level, ModerationLevel.fromId(level.id))
        }
    }
}
