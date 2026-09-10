package com.walcker.identity.api

import kotlinx.coroutines.flow.Flow

public interface SessionHolder {
    public val currentUser: Flow<UserSession?>
    public val isAuthenticated: Flow<Boolean>
}
