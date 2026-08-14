package com.walcker.games.features.data.cache

import com.walcker.games.features.domain.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory cache for matches.
 *
 * Provides an offline-first reactive stream of matches via [StateFlow].
 * Survives the lifetime of the application process (and DI scope).
 *
 * Phase1 intentionally avoids Room/SQLDelight to ship a working MVP
 * quickly. When persistence across app restarts becomes a requirement
 * we can swap this for a real database-backed implementation without
 * touching the repository layer — it implements the same contract.
 *
 * Thread safety: a [Mutex] guards mutations so concurrent refreshes
 * from different coroutines don't interleave writes.
 */
internal class InMemoryMatchCache {

    private val _matches = MutableStateFlow<List<Game>>(emptyList())
    val matches: StateFlow<List<Game>> = _matches.asStateFlow()

    private val mutex = Mutex()

    /**
     * Atomically replaces the cached set with [newMatches].
     */
    suspend fun replaceAll(newMatches: List<Game>) = mutex.withLock {
        _matches.value = newMatches
    }

    /**
     * Atomically adds or updates a single match in the cache.
     */
    suspend fun upsert(match: Game) = mutex.withLock {
        _matches.update { current ->
            val without = current.filterNot { it.id == match.id }
            without + match
        }
    }

    /**
     * Removes a single match from the cache.
     */
    suspend fun remove(id: String) = mutex.withLock {
        _matches.update { current -> current.filterNot { it.id == id } }
    }

    /**
     * Empties the cache.
     */
    suspend fun clear() = mutex.withLock {
        _matches.value = emptyList()
    }
}
