package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observa um único documento de partida em tempo real.
 *
 * Emite sempre que mudar:
 * - status (OPEN → FULL → FINISHED/CANCELLED)
 * - confirmedPlayers / vagas
 * - qualquer outro campo do documento
 *
 * @param matchId ID da partida
 * @return Flow que emite a partida atualizada
 */
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
