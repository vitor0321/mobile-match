package com.walcker.games.fake

import com.walcker.games.features.data.shared.source.AvailabilitySource
import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.model.Sport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeAvailabilitySource(
    var observeResult: Flow<Result<Availability>> = flowOf(Result.success(Availability.Unavailable)),
    var setAvailableResult: Result<Unit> = Result.success(Unit),
    var setAvailableSportsResult: Result<Unit> = Result.success(Unit),
) : AvailabilitySource {
    var setAvailableCallCount: Int = 0
        private set
    var setAvailableSportsCallCount: Int = 0
        private set

    override fun observe(userId: String): Flow<Result<Availability>> = observeResult

    override suspend fun setAvailable(
        userId: String,
        isAvailable: Boolean,
        availableUntilMs: Long?,
    ): Result<Unit> {
        setAvailableCallCount++
        return setAvailableResult
    }

    override suspend fun setAvailableSports(
        userId: String,
        sports: Set<Sport>,
    ): Result<Unit> {
        setAvailableSportsCallCount++
        return setAvailableSportsResult
    }
}
