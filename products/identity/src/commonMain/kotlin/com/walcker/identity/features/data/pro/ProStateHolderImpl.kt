package com.walcker.identity.features.data.pro

import com.walcker.identity.api.ProStateHolder
import com.walcker.identity.api.SessionHolder
import com.walcker.identity.api.TrialStatus
import com.walcker.identity.features.data.billing.BillingClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class ProStateHolderImpl(
    private val sessionHolder: SessionHolder,
    private val billingClient: BillingClient,
    private val cache: ProStateCache,
    ioDispatcher: CoroutineDispatcher,
) : ProStateHolder {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val state = MutableStateFlow(false)
    private val activeUid = MutableStateFlow<String?>(null)

    override val isPro: Flow<Boolean> = state.asStateFlow()

    override suspend fun checkTrialStatus(): TrialStatus {
        val user = sessionHolder.currentUser.first() ?: return TrialStatus.NotAuthenticated
        val uid = user.uid

        val cachedDate = cache.readRegistrationDate(uid)
        if (cachedDate != null) return cachedTrialStatus(cachedDate)

        val creationTimestamp = user.creationTimestamp
        if (creationTimestamp != null && creationTimestamp > 0) {
            val date = formatEpochMillisToDayKey(creationTimestamp)
            cache.saveRegistrationDate(uid, date)
            return cachedTrialStatus(date)
        }

        return TrialStatus.OfflineAndNoCachedDate
    }

    init {
        scope.launch {
            sessionHolder.currentUser
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    activeUid.value = uid
                    state.value = false
                    if (uid == null) {
                        billingClient.logOut()
                        return@collect
                    }

                    state.value = cache.read(uid)
                    billingClient.logIn(uid)
                        .onSuccess { isPro -> publishForActiveUser(uid, isPro) }
                }
        }
        scope.launch {
            billingClient.customerInfoUpdates()
                .distinctUntilChanged()
                .collect { update ->
                    publishForActiveUser(update.uid, update.isPro)
                }
        }
    }

    private suspend fun publishForActiveUser(uid: String, isPro: Boolean) {
        if (activeUid.value != uid) return
        state.value = isPro
        cache.save(uid, isPro)
    }

    private fun cachedTrialStatus(date: String): TrialStatus {
        val days = daysBetween(date, currentDayKey())
        return if (days <= 2) TrialStatus.Active else TrialStatus.Expired
    }
}
