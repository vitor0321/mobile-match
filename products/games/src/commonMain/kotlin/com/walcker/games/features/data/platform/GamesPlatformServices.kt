package com.walcker.games.features.data.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Platform-specific services for the games product.
 *
 * The actual implementations live in androidMain / iosMain and provide
 * DataStore instances configured with the platform-appropriate file paths.
 */
internal interface GamesPlatformServices {
    fun gamesPreferencesDataStore(): DataStore<Preferences>
}
