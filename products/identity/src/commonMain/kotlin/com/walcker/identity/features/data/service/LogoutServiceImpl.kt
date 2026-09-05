package com.walcker.identity.features.data.service

import com.walcker.identity.api.LogoutService
import com.walcker.identity.features.domain.usecase.SignUseCase

internal class LogoutServiceImpl(
    private val signUseCase: SignUseCase,
) : LogoutService {
    override suspend fun logout(): Result<Unit> = signUseCase.signOut()
}
