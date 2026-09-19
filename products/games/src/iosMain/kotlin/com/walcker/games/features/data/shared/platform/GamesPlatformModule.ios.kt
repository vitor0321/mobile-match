@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.walcker.games.features.data.shared.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual val gamesPlatformModule: Module =
    module {
        single<GamesPlatformServices> { IosGamesPlatformServices() }
    }

private class IosGamesPlatformServices : GamesPlatformServices {
    override fun gamesPreferencesDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath {
            val directory =
                requireNotNull(
                    NSFileManager.defaultManager
                        .URLForDirectory(
                            directory = NSDocumentDirectory,
                            inDomain = NSUserDomainMask,
                            appropriateForURL = null,
                            create = false,
                            error = null,
                        )?.path,
                )
            "$directory/datastore/games_preferences.preferences_pb".toPath()
        }
}
