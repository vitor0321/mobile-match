package com.walcker.games.features.data.shared.repository

import com.walcker.games.features.data.shared.source.AvailabilitySource
import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.repository.AvailabilityRepository
import kotlinx.coroutines.flow.Flow

internal class AvailabilityRepositoryImpl(
    private val source: AvailabilitySource,
) : AvailabilityRepository {
    override fun observe(userId: String): Flow<Result<Availability>> = source.observe(userId)

    override suspend fun setAvailable(
        userId: String,
        availability: Availability,
    ): Result<Unit> = source.setAvailable(userId, availability)
}
