package com.walcker.games.features.data.cache

import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResults
import com.walcker.games.fake.playerDetails
import com.walcker.games.fake.playerSearchResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InMemoryPlayerCacheTest {

    /** Controllable clock — the cache must be testable without sleeping. */
    private var now = 0L

    private fun cache(ttlMs: Long = InMemoryPlayerCache.DEFAULT_TTL_MS) =
        InMemoryPlayerCache(ttlMs = ttlMs, nowMs = { now })

    private val filters = PlayerSearchFilters(query = "ana")
    private val results = PlayerSearchResults(
        players = listOf(playerSearchResult()),
        reachedLimit = false,
    )

    @Test
    fun `returns null before anything is stored`() = runTest {
        val cache = cache()

        assertNull(cache.searchResults(filters))
        assertNull(cache.details("player-1"))
    }

    @Test
    fun `serves a stored search back`() = runTest {
        val cache = cache()
        cache.putSearchResults(filters, results)

        assertEquals(results, cache.searchResults(filters))
    }

    @Test
    fun `different filters are different entries`() = runTest {
        val cache = cache()
        cache.putSearchResults(filters, results)

        assertNull(cache.searchResults(filters.copy(query = "bruno")))
        assertNull(cache.searchResults(filters.copy(minRating = 4f)))
    }

    @Test
    fun `entry expires once the ttl elapses`() = runTest {
        val cache = cache(ttlMs = 1_000L)
        cache.putSearchResults(filters, results)

        now = 999L
        assertNotNull(cache.searchResults(filters))

        now = 1_000L
        assertNull(cache.searchResults(filters))
    }

    @Test
    fun `details expire independently`() = runTest {
        val cache = cache(ttlMs = 1_000L)
        cache.putDetails("player-1", playerDetails())

        now = 500L
        assertNotNull(cache.details("player-1"))

        now = 1_500L
        assertNull(cache.details("player-1"))
    }

    @Test
    fun `invalidating a player also drops every cached search`() = runTest {
        val cache = cache()
        cache.putSearchResults(filters, results)
        cache.putDetails("player-1", playerDetails(userId = "player-1"))
        cache.putDetails("player-2", playerDetails(userId = "player-2"))

        cache.invalidatePlayer("player-1")

        assertNull(cache.details("player-1"))
        // Searches are ordered by rating, so a new review can reshuffle lists
        // this player never appeared in.
        assertNull(cache.searchResults(filters))
        // Other players are untouched.
        assertNotNull(cache.details("player-2"))
    }

    @Test
    fun `clear empties everything`() = runTest {
        val cache = cache()
        cache.putSearchResults(filters, results)
        cache.putDetails("player-1", playerDetails())

        cache.clear()

        assertNull(cache.searchResults(filters))
        assertNull(cache.details("player-1"))
    }

    @Test
    fun `evicts the least recently written search past the cap`() = runTest {
        val cache = cache()
        val cap = InMemoryPlayerCache.MAX_CACHED_SEARCHES

        repeat(cap + 1) { index ->
            cache.putSearchResults(PlayerSearchFilters(query = "q$index"), results)
        }

        // The first one made room for the extra.
        assertNull(cache.searchResults(PlayerSearchFilters(query = "q0")))
        assertNotNull(cache.searchResults(PlayerSearchFilters(query = "q1")))
        assertNotNull(cache.searchResults(PlayerSearchFilters(query = "q$cap")))
    }

    @Test
    fun `rewriting a key refreshes it instead of duplicating`() = runTest {
        val cache = cache(ttlMs = 1_000L)
        cache.putSearchResults(filters, results)

        now = 900L
        cache.putSearchResults(filters, results)

        now = 1_500L
        // Still fresh: the second write reset the clock on this entry.
        assertNotNull(cache.searchResults(filters))
    }
}
