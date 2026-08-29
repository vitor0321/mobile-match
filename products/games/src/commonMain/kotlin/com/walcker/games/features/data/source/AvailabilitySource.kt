package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Availability
import kotlinx.coroutines.flow.Flow

internal interface AvailabilitySource {
    fun observe(userId: String): Flow<Result<Availability>>

    suspend fun setAvailable(userId: String, availability: Availability): Result<Unit>
}
