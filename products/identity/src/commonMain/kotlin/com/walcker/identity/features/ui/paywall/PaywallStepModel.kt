package com.walcker.identity.features.ui.paywall

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.api.ProStateHolder
import com.walcker.identity.api.SessionHolder
import com.walcker.identity.features.domain.billing.BillingRepository
import com.walcker.identity.features.domain.billing.ProductOffering
import com.walcker.identity.features.domain.billing.PurchaseError
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class PaywallStepModel(
    private val billingRepository: BillingRepository,
    private val sessionHolder: SessionHolder,
    private val proStateHolder: ProStateHolder,
    private val stringsHolder: IdentityStringsHolder,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : StateScreenModel<PaywallState>(PaywallState()) {
    private val eventChannel = Channel<PaywallInternalEvents>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        screenModelScope.launch {
            if (!sessionHolder.isAuthenticated.first()) {
                mutableState.value = mutableState.value.copy(isLoading = false)
                eventChannel.send(PaywallInternalEvents.RequireLogin)
                return@launch
            }
            loadOfferings()
        }
        screenModelScope.launch {
            proStateHolder.isPro.collect { isPro ->
                mutableState.value = mutableState.value.copy(isPro = isPro)
                if (isPro) {
                    refreshManagementUrl()
                } else {
                    mutableState.value = mutableState.value.copy(managementUrl = null)
                }
            }
        }
    }

    private suspend fun refreshManagementUrl() {
        billingRepository
            .managementUrl()
            .onSuccess { url ->
                mutableState.value = mutableState.value.copy(managementUrl = url)
            }
    }

    fun onEvent(event: PaywallInternalRoute) {
        when (event) {
            PaywallInternalRoute.OnBackClicked -> dismiss()
            PaywallInternalRoute.OnPurchaseClicked -> purchaseSelectedOffering()
            PaywallInternalRoute.OnRestoreClicked -> restorePurchases()
            PaywallInternalRoute.OnRetryClicked -> refreshOfferings()
            is PaywallInternalRoute.OnOfferingSelected -> selectOffering(event.offeringId)
        }
    }

    private fun refreshOfferings() {
        screenModelScope.launch { loadOfferings() }
    }

    private suspend fun loadOfferings() {
        mutableState.value =
            mutableState.value.copy(
                isLoading = true,
                error = null,
                errorMessage = null,
            )
        billingRepository
            .listOfferings()
            .onSuccess { offerings ->
                val selectedOfferingId =
                    mutableState.value.selectedOfferingId
                        ?.takeIf { selected -> offerings.any { it.id == selected } }
                        ?: offerings.firstOrNull()?.id
                val selectedOffering = offerings.firstOrNull { it.id == selectedOfferingId }
                mutableState.value =
                    mutableState.value.copy(
                        isLoading = false,
                        offerings = offerings,
                        selectedOfferingId = selectedOfferingId,
                        selectedOffering = selectedOffering,
                        selectedOfferingPeriod = selectedOffering?.resolveOfferingPeriod(),
                        errorMessage = null,
                    )
            }.onFailure { error ->
                crashReporter.recordException(error)
                val resolvedError = resolveError(error)
                mutableState.value =
                    mutableState.value.copy(
                        isLoading = false,
                        offerings = mutableState.value.offerings,
                        error = resolvedError,
                        errorMessage = resolveErrorMessage(resolvedError),
                    )
            }
    }

    private fun selectOffering(offeringId: String) {
        val selectedOffering = mutableState.value.offerings.firstOrNull { it.id == offeringId }
        mutableState.value =
            mutableState.value.copy(
                selectedOfferingId = offeringId,
                selectedOffering = selectedOffering,
                selectedOfferingPeriod = selectedOffering?.resolveOfferingPeriod(),
                error = null,
                errorMessage = null,
            )
    }

    private fun purchaseSelectedOffering() {
        screenModelScope.launch {
            if (!ensureAuthenticated()) return@launch
            val offering = selectedOffering() ?: return@launch
            mutableState.value =
                mutableState.value.copy(
                    purchaseInProgress = offering.packageId,
                    error = null,
                    errorMessage = null,
                )
            analytics.track(AnalyticsEvent.PurchaseAttempted(offering.packageId))
            billingRepository
                .purchase(offering.packageId)
                .onSuccess {
                    analytics.track(AnalyticsEvent.PurchaseResult(offering.packageId, success = true))
                    mutableState.value =
                        mutableState.value.copy(
                            purchaseInProgress = null,
                            errorMessage = null,
                        )
                    eventChannel.send(PaywallInternalEvents.Dismiss)
                }.onFailure { error ->
                    analytics.track(AnalyticsEvent.PurchaseResult(offering.packageId, success = false))
                    crashReporter.recordException(error)
                    val resolvedError = resolveError(error)
                    mutableState.value =
                        mutableState.value.copy(
                            purchaseInProgress = null,
                            error = resolvedError,
                            errorMessage = resolveErrorMessage(resolvedError),
                        )
                }
        }
    }

    private fun restorePurchases() {
        screenModelScope.launch {
            if (!ensureAuthenticated()) return@launch
            mutableState.value =
                mutableState.value.copy(
                    isRestoring = true,
                    error = null,
                    errorMessage = null,
                )
            billingRepository
                .restore()
                .onSuccess { hasProAccess ->
                    analytics.track(AnalyticsEvent.PurchaseRestored(success = true))
                    mutableState.value =
                        mutableState.value.copy(
                            isRestoring = false,
                            errorMessage = null,
                        )
                    val message =
                        if (hasProAccess) {
                            stringsHolder.strings.paywall.restoreSuccessMessage
                        } else {
                            stringsHolder.strings.paywall.restoreNoPurchasesMessage
                        }
                    eventChannel.send(PaywallInternalEvents.ShowSnackbar(message))
                }.onFailure { error ->
                    analytics.track(AnalyticsEvent.PurchaseRestored(success = false))
                    crashReporter.recordException(error)
                    val resolvedError = resolveError(error)
                    mutableState.value =
                        mutableState.value.copy(
                            isRestoring = false,
                            error = resolvedError,
                            errorMessage = resolveErrorMessage(resolvedError),
                        )
                }
        }
    }

    private suspend fun ensureAuthenticated(): Boolean {
        if (sessionHolder.currentUser.first() != null) return true
        eventChannel.send(PaywallInternalEvents.RequireLogin)
        return false
    }

    private fun selectedOffering(): ProductOffering? = mutableState.value.selectedOffering

    private fun ProductOffering.resolveOfferingPeriod(): PaywallOfferingPeriod? {
        val normalizedTitle = title.lowercase()
        val normalizedPackage = packageId.lowercase()

        return when {
            "month" in normalizedTitle ||
                "mensal" in normalizedTitle ||
                "month" in normalizedPackage ||
                "monthly" in normalizedPackage -> PaywallOfferingPeriod.MONTHLY

            "year" in normalizedTitle ||
                "annual" in normalizedTitle ||
                "anual" in normalizedTitle ||
                "year" in normalizedPackage ||
                "annual" in normalizedPackage -> PaywallOfferingPeriod.YEARLY

            else -> null
        }
    }

    private fun resolveError(error: Throwable): PaywallError =
        when (error) {
            PurchaseError.UserCancelled -> PaywallError.PurchaseCancelled
            PurchaseError.Network -> PaywallError.Network
            PurchaseError.ProductUnavailable -> PaywallError.ProductUnavailable
            PurchaseError.BillingUnavailable -> PaywallError.BillingUnavailable
            is PurchaseError.Unknown -> PaywallError.Generic(stringsHolder.strings.paywall.genericError)
            else -> PaywallError.Generic(stringsHolder.strings.paywall.genericError)
        }

    private fun resolveErrorMessage(error: PaywallError): String {
        val strings = stringsHolder.strings.paywall

        return when (error) {
            PaywallError.PurchaseCancelled -> strings.purchaseCancelledMessage
            PaywallError.Network -> strings.networkError
            PaywallError.ProductUnavailable -> strings.productUnavailableError
            PaywallError.BillingUnavailable -> strings.billingUnavailableError
            is PaywallError.Generic -> error.message
        }
    }

    private fun dismiss() {
        screenModelScope.launch {
            eventChannel.send(PaywallInternalEvents.Dismiss)
        }
    }
}
