package com.walcker.identity.features.data.remote

import com.walcker.identity.api.UserSession
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault

internal actual fun createAppleAuthSource(stringsHolder: IdentityStringsHolder): AppleAuthSource = AndroidAppleAuthSource(stringsHolder = stringsHolder)

internal actual val isAppleSignInAvailable: Boolean = false

internal class AndroidAppleAuthSource(
    private val stringsHolder: IdentityStringsHolder,
) : AppleAuthSource {
    override suspend fun signIn(): Result<UserSession> {
        val message = stringsHolder.resolveStringsOrDefault().nativeAuth.appleUnavailableOrCancelled
        return Result.failure(UnsupportedOperationException(message))
    }
}
