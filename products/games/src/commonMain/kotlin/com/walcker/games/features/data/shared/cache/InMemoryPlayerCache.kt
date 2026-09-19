package com.walcker.games.features.data.shared.cache

import com.walcker.games.features.domain.shared.model.PlayerDetails
import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.PlayerSearchResults
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class InMemoryPlayerCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val nowMs: () -> Long = {
        kotlin.time.Clock.System
            .now()
            .toEpochMilliseconds()
    },
) {
    private data class Entry<T>(
        val value: T,
        val storedAtMs: Long,
    )

    private val mutex = Mutex()
    private val searches = LinkedHashMap<PlayerSearchFilters, Entry<PlayerSearchResults>>()
    private val details = LinkedHashMap<String, Entry<PlayerDetails>>()

    suspend fun searchResults(filters: PlayerSearchFilters): PlayerSearchResults? = mutex.withLock { searches.read(filters) }

    suspend fun putSearchResults(
        filters: PlayerSearchFilters,
        results: PlayerSearchResults,
    ) = mutex.withLock {
        searches.write(filters, results, MAX_CACHED_SEARCHES)
    }

    suspend fun details(userId: String): PlayerDetails? = mutex.withLock { details.read(userId) }

    suspend fun putDetails(
        userId: String,
        player: PlayerDetails,
    ) = mutex.withLock {
        details.write(userId, player, MAX_CACHED_DETAILS)
    }

    suspend fun invalidatePlayer(userId: String) =
        mutex.withLock {
            details.remove(userId)
            searches.clear()
        }

    suspend fun clear() =
        mutex.withLock {
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

    private fun <K, V> LinkedHashMap<K, Entry<V>>.write(
        key: K,
        value: V,
        maxSize: Int,
    ) {
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

        internal const val MAX_CACHED_SEARCHES: Int = 24
        internal const val MAX_CACHED_DETAILS: Int = 32
    }
}
