package com.walcker.games.features.domain.shared.util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TeamShufflerTest {
    @Test
    fun `players up to capacity all appear exactly once in the output`() {
        val players = listOf("p1", "p2", "p3", "p4")

        val result = shuffleIntoTeams(players, teamCount = 2, playersPerTeam = 2, random = Random(seed = 1))

        assertEquals(players.toSet(), result.keys)
        assertEquals(players.size, result.size)
    }

    @Test
    fun `players beyond capacity are left out of the result`() {
        val players = (1..10).map { "p$it" }

        val result = shuffleIntoTeams(players, teamCount = 2, playersPerTeam = 3, random = Random(seed = 2))

        assertEquals(6, result.size)
        assertTrue(result.keys.all { it in players })
    }

    @Test
    fun `each team receives at most playersPerTeam members`() {
        val players = (1..11).map { "p$it" }

        val result = shuffleIntoTeams(players, teamCount = 3, playersPerTeam = 3, random = Random(seed = 3))

        val sizes = result.values.groupingBy { it }.eachCount()
        assertTrue(sizes.values.all { it <= 3 })
        assertEquals(9, result.size)
    }

    @Test
    fun `all assigned team indices are within range`() {
        val players = (1..20).map { "p$it" }

        val result = shuffleIntoTeams(players, teamCount = 4, playersPerTeam = 5, random = Random(seed = 4))

        assertTrue(result.values.all { it in 0 until 4 })
    }

    @Test
    fun `fewer players than total capacity leaves some teams partially filled`() {
        val players = (1..4).map { "p$it" }

        val result = shuffleIntoTeams(players, teamCount = 3, playersPerTeam = 2, random = Random(seed = 5))

        assertEquals(4, result.size)
        val sizes = result.values.groupingBy { it }.eachCount()
        assertTrue(sizes.size <= 3)
    }

    @Test
    fun `team count below 2 throws`() {
        assertFailsWith<IllegalArgumentException> {
            shuffleIntoTeams(listOf("p1"), teamCount = 1, playersPerTeam = 5)
        }
    }

    @Test
    fun `team count above 6 throws`() {
        assertFailsWith<IllegalArgumentException> {
            shuffleIntoTeams(listOf("p1"), teamCount = 7, playersPerTeam = 5)
        }
    }

    @Test
    fun `players per team below 2 throws`() {
        assertFailsWith<IllegalArgumentException> {
            shuffleIntoTeams(listOf("p1"), teamCount = 2, playersPerTeam = 1)
        }
    }

    @Test
    fun `players per team above 15 throws`() {
        assertFailsWith<IllegalArgumentException> {
            shuffleIntoTeams(listOf("p1"), teamCount = 2, playersPerTeam = 16)
        }
    }

    @Test
    fun `empty player list returns an empty map`() {
        val result = shuffleIntoTeams(emptyList(), teamCount = 2, playersPerTeam = 5)

        assertEquals(emptyMap(), result)
    }

    @Test
    fun `snake draft balances total skill across teams`() {
        val players = listOf("highA", "highB", "lowA", "lowB")
        val skillRatings = mapOf("highA" to 10f, "highB" to 10f, "lowA" to 1f, "lowB" to 1f)

        val result =
            shuffleIntoTeams(players, teamCount = 2, playersPerTeam = 2, skillRatings = skillRatings, random = Random(seed = 6))

        val totalSkillByTeam =
            result.entries
                .groupBy({ it.value }, { skillRatings.getValue(it.key) })
                .mapValues { (_, ratings) -> ratings.sum() }
        assertEquals(setOf(11f), totalSkillByTeam.values.toSet())
    }

    @Test
    fun `players without a skill rating are treated as beginners and can be excluded before a rated player`() {
        val players = listOf("ace", "rookieA", "rookieB", "rookieC", "rookieD")
        val skillRatings = mapOf("ace" to 10f)

        val result =
            shuffleIntoTeams(players, teamCount = 2, playersPerTeam = 2, skillRatings = skillRatings, random = Random(seed = 7))

        assertEquals(4, result.size)
        assertTrue("ace" in result.keys)
        val includedRookies = result.keys.count { it in setOf("rookieA", "rookieB", "rookieC", "rookieD") }
        assertEquals(3, includedRookies)
    }
}
