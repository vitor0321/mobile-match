package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.repository.PlayerRepository

internal interface GetPlayerDetailsUseCase {
    suspend operator fun invoke(userId: String): Result<PlayerDetails>
}

internal class GetPlayerDetailsUseCaseImpl(
    private val repository: PlayerRepository,
) : GetPlayerDetailsUseCase {
    override suspend fun invoke(userId: String): Result<PlayerDetails> {
        return repository.getPlayerDetails(userId)
    }
}
