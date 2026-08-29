package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.Availability
import com.walcker.games.features.domain.repository.AvailabilityRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveAvailabilityUseCase {
    operator fun invoke(userId: String): Flow<Result<Availability>>
}

internal class ObserveAvailabilityUseCaseImpl(
    private val repository: AvailabilityRepository,
) : ObserveAvailabilityUseCase {
    override operator fun invoke(userId: String): Flow<Result<Availability>> =
        repository.observe(userId)
}

internal interface SetAvailabilityUseCase {
    suspend operator fun invoke(userId: String, isAvailable: Boolean): Result<Unit>
}

internal class SetAvailabilityUseCaseImpl(
    private val repository: AvailabilityRepository,
) : SetAvailabilityUseCase {
    override suspend operator fun invoke(userId: String, isAvailable: Boolean): Result<Unit> =
        repository.setAvailable(
            userId = userId,
            availability = Availability(isAvailable = isAvailable, availableUntilMs = null),
        )
}
