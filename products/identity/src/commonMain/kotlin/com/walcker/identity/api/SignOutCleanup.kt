package com.walcker.identity.api

public fun interface SignOutCleanup {
    public suspend fun beforeSignOut()
}
