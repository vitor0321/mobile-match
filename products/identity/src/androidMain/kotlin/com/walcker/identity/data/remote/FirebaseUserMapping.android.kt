package com.walcker.identity.features.data.remote

import com.google.firebase.auth.FirebaseUser
import com.walcker.identity.api.UserSession

internal fun FirebaseUser.toUserSession(): UserSession =
    UserSession(
        uid = uid,
        email = email,
        displayName = displayName,
        creationTimestamp = metadata?.creationTimestamp,
        isEmailVerified = isEmailVerified,
        phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
    )
