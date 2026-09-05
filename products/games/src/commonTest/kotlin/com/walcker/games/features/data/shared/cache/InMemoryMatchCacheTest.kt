package com.walcker.games.features.data.shared.cache

import com.walcker.games.fake.game
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryMatchCacheTest {
    @Test
    fun `starts empty`() =
        runTest {
            assertTrue(InMemoryMatchCache().matches.value.isEmpty())
        }

    @Test
    fun `replaceAll swaps the whole list`() =
        runTest {
            val cache = InMemoryMatchCache()
            cache.replaceAll(listOf(game(id = "match-1"), game(id = "match-2")))

            cache.replaceAll(listOf(game(id = "match-3")))

            assertEquals(listOf("match-3"), cache.matches.value.map { it.id })
        }

    @Test
    fun `upsert adds a new match`() =
        runTest {
            val cache = InMemoryMatchCache()
            cache.replaceAll(listOf(game(id = "match-1")))

            cache.upsert(game(id = "match-2"))

            assertEquals(
                setOf("match-1", "match-2"),
                cache.matches.value
                    .map { it.id }
                    .toSet(),
            )
        }

    @Test
    fun `upsert replaces an existing match by id`() =
        runTest {
            val cache = InMemoryMatchCache()
            cache.replaceAll(listOf(game(id = "match-1", confirmedPlayers = 1)))

            cache.upsert(game(id = "match-1", confirmedPlayers = 9))

            assertEquals(1, cache.matches.value.size)
            assertEquals(
                9,
                cache.matches.value
                    .single()
                    .confirmedPlayers,
            )
        }

    @Test
    fun `remove drops the matching id and keeps the rest`() =
        runTest {
            val cache = InMemoryMatchCache()
            cache.replaceAll(listOf(game(id = "match-1"), game(id = "match-2")))

            cache.remove("match-1")

            assertEquals(listOf("match-2"), cache.matches.value.map { it.id })
        }

    @Test
    fun `clear empties the cache`() =
        runTest {
            val cache = InMemoryMatchCache()
            cache.replaceAll(listOf(game(id = "match-1")))

            cache.clear()

            assertTrue(cache.matches.value.isEmpty())
        }
}
