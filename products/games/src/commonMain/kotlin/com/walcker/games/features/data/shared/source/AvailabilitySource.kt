package com.walcker.games.features.data.shared.source

import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.model.Sport
import kotlinx.coroutines.flow.Flow

internal interface AvailabilitySource {
    fun observe(userId: String): Flow<Result<Availability>>

    suspend fun setAvailable(
        userId: String,
        isAvailable: Boolean,
        availableUntilMs: Long?,
    ): Result<Unit>

    suspend fun setAvailableSports(
        userId: String,
        sports: Set<Sport>,
    ): Result<Unit>
}
