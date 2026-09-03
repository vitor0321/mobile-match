package com.walcker.games.features.domain.playerProfile.usecase

import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.repository.AvailabilityRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveAvailabilityUseCase {
    operator fun invoke(userId: String): Flow<Result<Availability>>
}

internal class ObserveAvailabilityUseCaseImpl(
    private val repository: AvailabilityRepository,
) : ObserveAvailabilityUseCase {
    override operator fun invoke(userId: String): Flow<Result<Availability>> = repository.observe(userId)
}

internal interface SetAvailabilityUseCase {
    suspend operator fun invoke(
        userId: String,
        isAvailable: Boolean,
        availableUntilMs: Long? = null,
    ): Result<Unit>
}

internal class SetAvailabilityUseCaseImpl(
    private val repository: AvailabilityRepository,
) : SetAvailabilityUseCase {
    override suspend operator fun invoke(
        userId: String,
        isAvailable: Boolean,
        availableUntilMs: Long?,
    ): Result<Unit> =
        repository.setAvailable(
            userId = userId,
            isAvailable = isAvailable,
            availableUntilMs = availableUntilMs,
        )
}

internal interface SetAvailableSportsUseCase {
    suspend operator fun invoke(
        userId: String,
        sports: Set<Sport>,
    ): Result<Unit>
}

internal class SetAvailableSportsUseCaseImpl(
    private val repository: AvailabilityRepository,
) : SetAvailableSportsUseCase {
    override suspend operator fun invoke(
        userId: String,
        sports: Set<Sport>,
    ): Result<Unit> = repository.setAvailableSports(userId, sports)
}
