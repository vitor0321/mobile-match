package com.walcker.identity.fake

import com.walcker.identity.api.SessionHolder
import com.walcker.identity.api.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeSessionHolder(
    initialUser: UserSession? =
        UserSession(
            uid = "uid-1",
            email = "user@match.app",
            displayName = "Match User",
        ),
) : SessionHolder {
    private val currentUserState = MutableStateFlow(initialUser)
    private val isAuthenticatedState = MutableStateFlow(initialUser != null)

    override val currentUser: Flow<UserSession?> = currentUserState
    override val isAuthenticated: Flow<Boolean> = isAuthenticatedState

    fun updateUser(userSession: UserSession?) {
        currentUserState.value = userSession
        isAuthenticatedState.value = userSession != null
    }
}
