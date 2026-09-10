package com.walcker.games.features.domain.shared.util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TeamShufflerTest {
    @Test
    fun `every input player appears exactly once in the output`() {
        val players = listOf("p1", "p2", "p3", "p4", "p5")

        val result = shuffleIntoTeams(players, teamCount = 2, random = Random(seed = 1))

        assertEquals(players.toSet(), result.keys)
        assertEquals(players.size, result.size)
    }

    @Test
    fun `teams are balanced within one player of each other`() {
        val players = (1..11).map { "p$it" }

        val result = shuffleIntoTeams(players, teamCount = 3, random = Random(seed = 2))

        val sizes = result.values.groupingBy { it }.eachCount().values
        assertTrue(sizes.max() - sizes.min() <= 1)
    }

    @Test
    fun `all assigned team indices are within range`() {
        val players = (1..7).map { "p$it" }

        val result = shuffleIntoTeams(players, teamCount = 4, random = Random(seed = 3))

        assertTrue(result.values.all { it in 0 until 4 })
    }

    @Test
    fun `team count below 1 throws`() {
        assertFailsWith<IllegalArgumentException> {
            shuffleIntoTeams(listOf("p1"), teamCount = 0)
        }
    }

    @Test
    fun `team count of 1 throws`() {
        assertFailsWith<IllegalArgumentException> {
            shuffleIntoTeams(listOf("p1"), teamCount = 1)
        }
    }

    @Test
    fun `team count above 6 throws`() {
        assertFailsWith<IllegalArgumentException> {
            shuffleIntoTeams(listOf("p1"), teamCount = 7)
        }
    }

    @Test
    fun `empty player list returns an empty map`() {
        val result = shuffleIntoTeams(emptyList(), teamCount = 2)

        assertEquals(emptyMap(), result)
    }
}
