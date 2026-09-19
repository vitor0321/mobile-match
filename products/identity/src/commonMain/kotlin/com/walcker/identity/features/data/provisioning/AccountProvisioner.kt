package com.walcker.identity.features.data.provisioning

import com.walcker.identity.api.AccountProvisioning
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

private const val ENSURE_USER_PROVISIONED = "ensureUserProvisioned"

internal class AccountProvisioner(
    private val sessionHolder: SessionHolder,
    private val firestore: FirestoreClient,
    private val crashReporter: CrashReporter,
) : AccountProvisioning {
    private val provisionedUids = mutableSetOf<String>()

    override suspend fun start() {
        sessionHolder.currentUser
            .filterNotNull()
            .map { session -> session.uid }
            .distinctUntilChanged()
            .collect { uid ->
                if (uid in provisionedUids) return@collect
                firestore
                    .callFunction(ENSURE_USER_PROVISIONED, emptyMap())
                    .onSuccess { provisionedUids += uid }
                    .onFailure { error -> crashReporter.recordException(error) }
            }
    }
}
