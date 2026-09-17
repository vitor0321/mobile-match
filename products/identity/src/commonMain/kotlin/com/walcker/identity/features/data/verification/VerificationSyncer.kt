package com.walcker.identity.features.data.verification

import com.walcker.identity.api.SessionHolder
import com.walcker.identity.api.VerificationSync
import com.walcker.identity.features.data.remote.VerificationSyncCallableSource
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

internal class VerificationSyncer(
    private val sessionHolder: SessionHolder,
    private val callableSource: VerificationSyncCallableSource,
    private val crashReporter: CrashReporter,
) : VerificationSync {
    private val syncedSnapshots = mutableSetOf<VerificationSnapshot>()

    override suspend fun start() {
        sessionHolder.currentUser
            .filterNotNull()
            .map { session ->
                VerificationSnapshot(
                    uid = session.uid,
                    isEmailVerified = session.isEmailVerified,
                    phoneNumber = session.phoneNumber,
                )
            }.distinctUntilChanged()
            .collect { snapshot ->
                if (!snapshot.hasAnyVerification() || snapshot in syncedSnapshots) return@collect
                callableSource
                    .syncVerificationStatus()
                    .onSuccess { syncedSnapshots += snapshot }
                    .onFailure { error -> crashReporter.recordException(error) }
            }
    }
}

private data class VerificationSnapshot(
    val uid: String,
    val isEmailVerified: Boolean,
    val phoneNumber: String?,
) {
    fun hasAnyVerification(): Boolean = isEmailVerified || !phoneNumber.isNullOrBlank()
}
