package com.walcker.games.features.data.repository

import com.walcker.games.features.data.source.AvailabilitySource
import com.walcker.games.features.domain.model.Availability
import com.walcker.games.features.domain.repository.AvailabilityRepository
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
