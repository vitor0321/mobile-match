package com.walcker.identity.api

import kotlinx.coroutines.flow.Flow

public sealed class TrialStatus {
    public data object Active : TrialStatus()

    public data object Expired : TrialStatus()

    public data object NotAuthenticated : TrialStatus()

    public data object OfflineAndNoCachedDate : TrialStatus()
}

public interface ProStateHolder {
    public val isPro: Flow<Boolean>

    public suspend fun checkTrialStatus(): TrialStatus
}
