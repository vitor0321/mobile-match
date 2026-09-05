package com.walcker.identity.fake

import com.walcker.identity.api.ProStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeProStateHolder(
    initialIsPro: Boolean = false,
) : ProStateHolder {
    private val state = MutableStateFlow(initialIsPro)

    override val isPro: Flow<Boolean> = state

    var trialStatusResult: com.walcker.identity.api.TrialStatus = com.walcker.identity.api.TrialStatus.Active

    override suspend fun checkTrialStatus(): com.walcker.identity.api.TrialStatus = trialStatusResult

    fun update(value: Boolean) {
        state.value = value
    }
}
