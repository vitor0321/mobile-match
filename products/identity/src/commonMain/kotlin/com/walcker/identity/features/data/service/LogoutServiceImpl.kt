package com.walcker.identity.features.data.service

import com.walcker.identity.api.LogoutService
import com.walcker.identity.api.SignOutCleanup
import com.walcker.identity.features.domain.usecase.SignUseCase
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val CLEANUP_TIMEOUT = 3.seconds

internal class LogoutServiceImpl(
    private val signUseCase: SignUseCase,
    private val cleanups: List<SignOutCleanup> = emptyList(),
) : LogoutService {
    override suspend fun logout(): Result<Unit> {
        cleanups.forEach { cleanup -> withTimeoutOrNull(CLEANUP_TIMEOUT) { cleanup.beforeSignOut() } }
        return signUseCase.signOut()
    }
}
