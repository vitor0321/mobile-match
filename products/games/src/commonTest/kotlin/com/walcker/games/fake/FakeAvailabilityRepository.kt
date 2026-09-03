package com.walcker.games.fake

import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.repository.AvailabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class SetAvailableCall(
    val userId: String,
    val isAvailable: Boolean,
    val availableUntilMs: Long?,
)

internal class FakeAvailabilityRepository(
    var setAvailableResult: Result<Unit> = Result.success(Unit),
    var setAvailableSportsResult: Result<Unit> = Result.success(Unit),
) : AvailabilityRepository {
    private val availabilityFlow = MutableStateFlow<Result<Availability>>(Result.success(Availability.Unavailable))

    val setAvailableCalls: MutableList<SetAvailableCall> = mutableListOf()
    val setAvailableSportsCalls: MutableList<Pair<String, Set<Sport>>> = mutableListOf()

    fun emit(result: Result<Availability>) {
        availabilityFlow.value = result
    }

    override fun observe(userId: String) = availabilityFlow.asStateFlow()

    override suspend fun setAvailable(
        userId: String,
        isAvailable: Boolean,
        availableUntilMs: Long?,
    ): Result<Unit> {
        setAvailableCalls += SetAvailableCall(userId, isAvailable, availableUntilMs)
        return setAvailableResult
    }

    override suspend fun setAvailableSports(
        userId: String,
        sports: Set<Sport>,
    ): Result<Unit> {
        setAvailableSportsCalls += userId to sports
        return setAvailableSportsResult
    }
}
