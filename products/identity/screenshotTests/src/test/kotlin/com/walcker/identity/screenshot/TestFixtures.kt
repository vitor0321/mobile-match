@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import com.walcker.identity.api.UserSession

internal val fakeUserSession =
    UserSession(
        uid = "uid-1",
        email = "user@match.app",
        displayName = "Match User",
    )
