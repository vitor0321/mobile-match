@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.walcker.identity.features.data.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.GoogleSignIn.GIDSignIn
import com.walcker.identity.features.data.remote.GoogleAuthSource
import com.walcker.identity.features.data.remote.IosGoogleAuthSource
import com.walcker.identity.strings.IdentityStringsHolder
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual val identityPlatformModule: Module =
    module {
        single<IdentityPlatformServices> { IosIdentityPlatformServices(stringsHolder = get()) }
    }

private class IosIdentityPlatformServices(
    private val stringsHolder: IdentityStringsHolder,
) : IdentityPlatformServices {
    override fun proStateDataStore(): DataStore<Preferences> =
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
            "$directory/datastore/identity_pro_state.preferences_pb".toPath()
        }

    override fun googleAuthSource(): GoogleAuthSource =
        IosGoogleAuthSource(
            auth = FIRAuth.auth(),
            signIn = GIDSignIn.sharedInstance,
            stringsHolder = stringsHolder,
        )
}
