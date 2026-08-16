package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observa a lista de participantes de uma partida em tempo real.
 *
 * Emite sempre que houver mudança em:
 * - Alguém entra/sai da partida
 * - Alguém é promovido da waitlist para confirmed
 * - Alguém paga
 *
 * Para parar de observar, basta cancelar a collection da Flow.
 *
 * @param matchId ID da partida
 * @return Flow que emite o resumo atualizado dos participantes
 */
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
