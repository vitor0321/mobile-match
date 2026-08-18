package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.repository.PlayerRepository

/**
 * Use case for fetching detailed player profile.
 *
 * Called when user taps on a player from search results.
 * Returns complete profile with stats, location, membership date, etc.
 */
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
