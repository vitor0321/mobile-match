package com.walcker.identity.features.data.remote

import com.walcker.identity.api.UserSession

internal interface GoogleAuthSource {
    suspend fun signIn(): Result<UserSession>
}
