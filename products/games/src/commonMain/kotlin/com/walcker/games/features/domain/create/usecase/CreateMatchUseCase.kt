package com.walcker.games.features.domain.create.usecase

import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.repository.GameRepository

internal interface CreateMatchUseCase {
    suspend operator fun invoke(request: CreateMatchRequest): Result<String>
}

internal class CreateMatchUseCaseImpl(
    private val repository: GameRepository,
) : CreateMatchUseCase {
    override suspend fun invoke(request: CreateMatchRequest): Result<String> = repository.createMatch(request)
}
