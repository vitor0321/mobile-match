package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.PlayerDetails
import com.walcker.games.features.domain.shared.repository.PlayerRepository

internal interface GetPlayerDetailsUseCase {
    suspend operator fun invoke(userId: String): Result<PlayerDetails>
}

internal class GetPlayerDetailsUseCaseImpl(
    private val repository: PlayerRepository,
) : GetPlayerDetailsUseCase {
    override suspend fun invoke(userId: String): Result<PlayerDetails> = repository.getPlayerDetails(userId)
}
