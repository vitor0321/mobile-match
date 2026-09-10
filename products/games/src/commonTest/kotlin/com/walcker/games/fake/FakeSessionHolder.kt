package com.walcker.games.fake

import com.walcker.identity.api.SessionHolder
import com.walcker.identity.api.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

internal class FakeSessionHolder(
    session: UserSession? = testUserSession(),
) : SessionHolder {
    private val sessionFlow = MutableStateFlow(session)

    override val currentUser = sessionFlow.asStateFlow()
    override val isAuthenticated = sessionFlow.map { it != null }

    fun setSession(session: UserSession?) {
        sessionFlow.value = session
    }
}

internal fun testUserSession(
    uid: String = "user-1",
    email: String? = "ana@example.com",
    displayName: String? = "Ana Souza",
): UserSession =
    UserSession(
        uid = uid,
        email = email,
        displayName = displayName,
    )
