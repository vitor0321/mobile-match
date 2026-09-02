package com.walcker.games.fake

import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.repository.AvailabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeAvailabilityRepository(
    var setAvailableResult: Result<Unit> = Result.success(Unit),
) : AvailabilityRepository {
    private val availabilityFlow = MutableStateFlow<Result<Availability>>(Result.success(Availability.Unavailable))

    val setAvailableCalls: MutableList<Pair<String, Availability>> = mutableListOf()

    fun emit(result: Result<Availability>) {
        availabilityFlow.value = result
    }

    override fun observe(userId: String) = availabilityFlow.asStateFlow()

    override suspend fun setAvailable(
        userId: String,
        availability: Availability,
    ): Result<Unit> {
        setAvailableCalls += userId to availability
        return setAvailableResult
    }
}
