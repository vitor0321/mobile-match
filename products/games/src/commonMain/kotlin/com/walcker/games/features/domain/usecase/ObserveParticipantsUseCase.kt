package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveParticipantsUseCase {
    operator fun invoke(matchId: String): Flow<Result<ParticipantsSummary>>
}

internal class ObserveParticipantsUseCaseImpl(
    private val repository: GameRepository,
) : ObserveParticipantsUseCase {

    override fun invoke(matchId: String): Flow<Result<ParticipantsSummary>> {
        return repository.observeParticipants(matchId)
    }
}
