package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.repository.GameRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveMatchUseCase {
    operator fun invoke(matchId: String): Flow<Result<Game>>
}

internal class ObserveMatchUseCaseImpl(
    private val repository: GameRepository,
) : ObserveMatchUseCase {
    override fun invoke(matchId: String): Flow<Result<Game>> = repository.observeMatch(matchId)
}
