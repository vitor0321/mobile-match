package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.Availability
import com.walcker.games.features.domain.repository.AvailabilityRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observa a disponibilidade do próprio usuário.
 */
internal interface ObserveAvailabilityUseCase {
    operator fun invoke(userId: String): Flow<Result<Availability>>
}

internal class ObserveAvailabilityUseCaseImpl(
    private val repository: AvailabilityRepository,
) : ObserveAvailabilityUseCase {
    override operator fun invoke(userId: String): Flow<Result<Availability>> =
        repository.observe(userId)
}

/**
 * Liga ou desliga o toggle.
 */
internal interface SetAvailabilityUseCase {
    suspend operator fun invoke(userId: String, isAvailable: Boolean): Result<Unit>
}

internal class SetAvailabilityUseCaseImpl(
    private val repository: AvailabilityRepository,
) : SetAvailabilityUseCase {
    /**
     * Ligar grava sem vencimento — "até eu desligar". Desligar limpa a janela
     * junto, senão sobraria um `availableUntil` no futuro num documento que diz
     * indisponível: inconsistente para quem for depurar, e uma armadilha para
     * qualquer código que um dia olhe só a data.
     */
    override suspend operator fun invoke(userId: String, isAvailable: Boolean): Result<Unit> =
        repository.setAvailable(
            userId = userId,
            availability = Availability(isAvailable = isAvailable, availableUntilMs = null),
        )
}
