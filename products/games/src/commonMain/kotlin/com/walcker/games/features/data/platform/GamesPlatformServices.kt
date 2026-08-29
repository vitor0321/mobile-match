package com.walcker.games.features.data.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal interface GamesPlatformServices {
    fun gamesPreferencesDataStore(): DataStore<Preferences>
}
