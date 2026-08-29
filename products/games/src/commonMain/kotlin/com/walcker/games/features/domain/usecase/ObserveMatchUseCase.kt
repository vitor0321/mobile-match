package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveMatchUseCase {
    operator fun invoke(matchId: String): Flow<Result<Game>>
}

internal class ObserveMatchUseCaseImpl(
    private val repository: GameRepository,
) : ObserveMatchUseCase {

    override fun invoke(matchId: String): Flow<Result<Game>> {
        return repository.observeMatch(matchId)
    }
}
