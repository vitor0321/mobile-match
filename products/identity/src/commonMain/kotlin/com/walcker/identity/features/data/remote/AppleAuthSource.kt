package com.walcker.identity.features.data.remote

import com.walcker.identity.api.UserSession
import com.walcker.identity.strings.IdentityStringsHolder

internal interface AppleAuthSource {
    suspend fun signIn(): Result<UserSession>

    suspend fun reauthenticate(): Result<Unit>
}

internal expect fun createAppleAuthSource(stringsHolder: IdentityStringsHolder): AppleAuthSource

internal expect val isAppleSignInAvailable: Boolean
