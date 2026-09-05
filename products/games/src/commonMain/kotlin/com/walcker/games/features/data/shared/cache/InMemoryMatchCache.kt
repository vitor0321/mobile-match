package com.walcker.games.features.data.shared.cache

import com.walcker.games.features.domain.shared.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class InMemoryMatchCache {
    private val _matches = MutableStateFlow<List<Game>>(emptyList())
    val matches: StateFlow<List<Game>> = _matches.asStateFlow()

    private val mutex = Mutex()

    suspend fun replaceAll(newMatches: List<Game>) =
        mutex.withLock {
            _matches.value = newMatches
        }

    suspend fun appendAll(newMatches: List<Game>) =
        mutex.withLock {
            val existingIds = _matches.value.mapTo(mutableSetOf()) { it.id }
            _matches.value = _matches.value + newMatches.filterNot { it.id in existingIds }
        }

    suspend fun upsert(match: Game) =
        mutex.withLock {
            _matches.update { current ->
                val without = current.filterNot { it.id == match.id }
                without + match
            }
        }

    suspend fun remove(id: String) =
        mutex.withLock {
            _matches.update { current -> current.filterNot { it.id == id } }
        }

    suspend fun clear() =
        mutex.withLock {
            _matches.value = emptyList()
        }
}
