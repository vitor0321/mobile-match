package com.walcker.identity.features.data.platform

import android.app.Application
import androidx.credentials.CredentialManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.firebase.auth.FirebaseAuth
import com.walcker.identity.features.data.remote.AndroidGoogleAuthSource
import com.walcker.identity.features.data.remote.GoogleAuthSource
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.navigation.CurrentActivityHolder
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val identityPlatformModule: Module = module {
    single<IdentityPlatformServices> {
        AndroidIdentityPlatformServices(
            application = androidContext().applicationContext as Application,
            activityHolder = get(),
            stringsHolder = get(),
        )
    }
}

private class AndroidIdentityPlatformServices(
    private val application: Application,
    private val activityHolder: CurrentActivityHolder,
    private val stringsHolder: IdentityStringsHolder,
) : IdentityPlatformServices {
    override fun proStateDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath {
        (application.filesDir.path + "/datastore/identity_pro_state.preferences_pb").toPath()
    }

    override fun googleAuthSource(): GoogleAuthSource = AndroidGoogleAuthSource(
        application = application,
        firebaseAuth = FirebaseAuth.getInstance(),
        credentialManager = CredentialManager.create(application),
        activityHolder = activityHolder,
        stringsHolder = stringsHolder,
    )
}
