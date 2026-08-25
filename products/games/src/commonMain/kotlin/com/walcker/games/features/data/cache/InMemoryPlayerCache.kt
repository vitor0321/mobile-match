package com.walcker.games.features.data.cache

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResults
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Short-lived cache for player search results and profiles.
 *
 * Player data changes slowly, but the search screen re-queries on every filter
 * tweak and every back-navigation from a profile. A five minute TTL kills that
 * repetition without letting a stale name or rating linger for a whole session.
 *
 * Unlike [InMemoryMatchCache] this is not a reactive [kotlinx.coroutines.flow.StateFlow]:
 * a search result set is a snapshot answering one question, not a stream the UI
 * observes. Reads return `null` on a miss so the repository falls through to
 * the network.
 *
 * Thread safety: a [Mutex] guards every access.
 *
 * @param ttlMs how long an entry stays fresh
 * @param nowMs injectable clock — tests advance time instead of sleeping
 */
internal class InMemoryPlayerCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {

    private data class Entry<T>(val value: T, val storedAtMs: Long)

    private val mutex = Mutex()
    private val searches = LinkedHashMap<PlayerSearchFilters, Entry<PlayerSearchResults>>()
    private val details = LinkedHashMap<String, Entry<PlayerDetails>>()

    /** Cached results for [filters], or `null` when absent or expired. */
    suspend fun searchResults(filters: PlayerSearchFilters): PlayerSearchResults? =
        mutex.withLock { searches.read(filters) }

    suspend fun putSearchResults(
        filters: PlayerSearchFilters,
        results: PlayerSearchResults,
    ) = mutex.withLock {
        searches.write(filters, results, MAX_CACHED_SEARCHES)
    }

    /** Cached profile for [userId], or `null` when absent or expired. */
    suspend fun details(userId: String): PlayerDetails? =
        mutex.withLock { details.read(userId) }

    suspend fun putDetails(userId: String, player: PlayerDetails) = mutex.withLock {
        details.write(userId, player, MAX_CACHED_DETAILS)
    }

    /**
     * Forgets one player. Every cached search goes too: results are ordered by
     * rating, so a new review can reshuffle lists this player never appeared in.
     */
    suspend fun invalidatePlayer(userId: String) = mutex.withLock {
        details.remove(userId)
        searches.clear()
    }

    /** Drops everything. Call on sign-out. */
    suspend fun clear() = mutex.withLock {
        searches.clear()
        details.clear()
    }

    private fun <K, V> MutableMap<K, Entry<V>>.read(key: K): V? {
        val entry = this[key] ?: return null
        if (isExpired(entry)) {
            remove(key)
            return null
        }
        return entry.value
    }

    private fun <K, V> LinkedHashMap<K, Entry<V>>.write(key: K, value: V, maxSize: Int) {
        // Re-inserting moves the key to the end, so the eviction below always
        // drops the least recently written entry.
        remove(key)
        put(key, Entry(value, nowMs()))

        if (size <= maxSize) return
        entries.removeAll { isExpired(it.value) }
        while (size > maxSize) {
            val oldest = keys.firstOrNull() ?: break
            remove(oldest)
        }
    }

    private fun <V> isExpired(entry: Entry<V>): Boolean = nowMs() - entry.storedAtMs >= ttlMs

    internal companion object {
        internal const val DEFAULT_TTL_MS: Long = 5 * 60 * 1000L

        /**
         * Every distinct filter combination is a key, so an unbounded map would
         * grow with each keystroke the user makes.
         */
        internal const val MAX_CACHED_SEARCHES: Int = 24
        internal const val MAX_CACHED_DETAILS: Int = 32
    }
}
