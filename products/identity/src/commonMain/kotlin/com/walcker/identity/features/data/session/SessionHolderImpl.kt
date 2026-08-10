package com.walcker.identity.features.data.session

import com.walcker.identity.api.SessionHolder
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SessionHolderImpl(
    authRepository: AuthRepository,
) : SessionHolder {
    override val currentUser: Flow<UserSession?> = authRepository.currentUser
    override val isAuthenticated: Flow<Boolean> = authRepository.currentUser
        .map { it != null }
        .distinctUntilChanged()
}

