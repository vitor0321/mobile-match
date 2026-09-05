package com.walcker.games.features.data.shared.repository

import com.walcker.games.features.data.shared.source.AvailabilitySource
import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.repository.AvailabilityRepository
import kotlinx.coroutines.flow.Flow

internal class AvailabilityRepositoryImpl(
    private val source: AvailabilitySource,
) : AvailabilityRepository {
    override fun observe(userId: String): Flow<Result<Availability>> = source.observe(userId)

    override suspend fun setAvailable(
        userId: String,
        isAvailable: Boolean,
        availableUntilMs: Long?,
    ): Result<Unit> = source.setAvailable(userId, isAvailable, availableUntilMs)

    override suspend fun setAvailableSports(
        userId: String,
        sports: Set<Sport>,
    ): Result<Unit> = source.setAvailableSports(userId, sports)
}
