package com.walcker.identity.api

public interface LogoutService {
    suspend fun logout(): Result<Unit>
}
