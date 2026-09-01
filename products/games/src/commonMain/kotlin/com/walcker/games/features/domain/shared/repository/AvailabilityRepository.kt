package com.walcker.games.features.domain.shared.repository

import com.walcker.games.features.domain.shared.model.Availability
import kotlinx.coroutines.flow.Flow

internal interface AvailabilityRepository {
    fun observe(userId: String): Flow<Result<Availability>>

    suspend fun setAvailable(
        userId: String,
        availability: Availability,
    ): Result<Unit>
}
