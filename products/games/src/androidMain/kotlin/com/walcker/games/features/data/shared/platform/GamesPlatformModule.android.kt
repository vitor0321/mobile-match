package com.walcker.games.features.data.shared.platform

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val gamesPlatformModule: Module =
    module {
        single<GamesPlatformServices> {
            AndroidGamesPlatformServices(
                application = androidContext().applicationContext as Application,
            )
        }
    }

private class AndroidGamesPlatformServices(
    private val application: Application,
) : GamesPlatformServices {
    override fun gamesPreferencesDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath {
            (application.filesDir.path + "/datastore/games_preferences.preferences_pb").toPath()
        }
}
