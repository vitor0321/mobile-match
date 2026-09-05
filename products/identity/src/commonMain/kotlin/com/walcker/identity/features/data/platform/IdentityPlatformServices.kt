package com.walcker.identity.features.data.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.walcker.identity.features.data.remote.GoogleAuthSource

internal interface IdentityPlatformServices {
    fun proStateDataStore(): DataStore<Preferences>

    fun googleAuthSource(): GoogleAuthSource
}
