package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.Availability
import kotlinx.coroutines.flow.Flow

/**
 * Disponibilidade do próprio usuário (regra B5).
 */
internal interface AvailabilityRepository {
    fun observe(userId: String): Flow<Result<Availability>>

    suspend fun setAvailable(userId: String, availability: Availability): Result<Unit>
}
