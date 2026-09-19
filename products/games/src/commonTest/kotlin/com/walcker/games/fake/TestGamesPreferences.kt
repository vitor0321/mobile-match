package com.walcker.games.fake

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.walcker.games.features.data.home.preferences.GamesPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class InMemoryPreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        mutex.withLock {
            val updated = transform(state.value)
            state.value = updated
            updated
        }
}

private val TEST_KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")

internal fun testGamesPreferences(hasSyncedBefore: Boolean = true): GamesPreferences {
    val initial =
        if (hasSyncedBefore) {
            mutablePreferencesOf(TEST_KEY_LAST_SYNC_AT to 0L)
        } else {
            emptyPreferences()
        }
    return GamesPreferences(dataStore = InMemoryPreferencesDataStore(initial))
}
