package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.repository.GameRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveParticipantsUseCase {
    operator fun invoke(matchId: String): Flow<Result<ParticipantsSummary>>
}

internal class ObserveParticipantsUseCaseImpl(
    private val repository: GameRepository,
) : ObserveParticipantsUseCase {
    override fun invoke(matchId: String): Flow<Result<ParticipantsSummary>> = repository.observeParticipants(matchId)
}
