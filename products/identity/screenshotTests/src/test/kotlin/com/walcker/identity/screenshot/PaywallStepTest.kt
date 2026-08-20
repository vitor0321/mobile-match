@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.domain.billing.ProductOffering
import com.walcker.identity.features.ui.paywall.PaywallError
import com.walcker.identity.features.ui.paywall.PaywallOfferingPeriod
import com.walcker.identity.features.ui.paywall.PaywallScreen
import com.walcker.identity.features.ui.paywall.PaywallState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test

class PaywallStepTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val monthlyOffering = ProductOffering(
        id = "default:${'$'}rc_monthly",
        offeringId = "default",
        packageId = "${'$'}rc_monthly",
        title = "Plano mensal",
        description = "Apoie o Match e tenha acesso antecipado ao que vem.",
        priceLabel = "R$ 14,90",
    )

    private val annualOffering = ProductOffering(
        id = "default:${'$'}rc_annual",
        offeringId = "default",
        packageId = "${'$'}rc_annual",
        title = "Plano anual",
        description = "Melhor custo-benefício para usar o Match todos os dias.",
        priceLabel = "R$ 119,90",
    )

    private val loadedState = PaywallState(
        isLoading = false,
        offerings = listOf(monthlyOffering, annualOffering).toImmutableList(),
        selectedOfferingId = annualOffering.id,
        selectedOffering = annualOffering,
        selectedOfferingPeriod = PaywallOfferingPeriod.YEARLY,
    )

    private val monthlySelectedState = PaywallState(
        isLoading = false,
        offerings = listOf(monthlyOffering, annualOffering).toImmutableList(),
        selectedOfferingId = monthlyOffering.id,
        selectedOffering = monthlyOffering,
        selectedOfferingPeriod = PaywallOfferingPeriod.MONTHLY,
    )

    private val loadingState = PaywallState(
        isLoading = true,
        offerings = persistentListOf(),
    )

    private val emptyState = PaywallState(
        isLoading = false,
        offerings = persistentListOf(),
        error = null,
    )

    private val errorNetworkState = PaywallState(
        isLoading = false,
        offerings = persistentListOf(),
        error = PaywallError.Network,
        errorMessage = "Não foi possível acessar o billing agora.",
    )

    private val purchaseInProgressState = loadedState.copy(
        purchaseInProgress = annualOffering.id,
    )

    private val restoreInProgressState = loadedState.copy(
        isRestoring = true,
    )

    private val singleOfferingState = PaywallState(
        isLoading = false,
        offerings = listOf(annualOffering).toImmutableList(),
        selectedOfferingId = annualOffering.id,
        selectedOffering = annualOffering,
        selectedOfferingPeriod = PaywallOfferingPeriod.YEARLY,
    )

    private val proCustomerState = PaywallState(
        isLoading = false,
        offerings = listOf(monthlyOffering, annualOffering).toImmutableList(),
        selectedOfferingId = annualOffering.id,
        selectedOffering = annualOffering,
        selectedOfferingPeriod = PaywallOfferingPeriod.YEARLY,
        isPro = true,
        managementUrl = "https://play.google.com/store/account/subscriptions",
    )

    private val proCustomerManagementUnavailableState = PaywallState(
        isLoading = false,
        offerings = listOf(monthlyOffering, annualOffering).toImmutableList(),
        selectedOfferingId = annualOffering.id,
        selectedOffering = annualOffering,
        selectedOfferingPeriod = PaywallOfferingPeriod.YEARLY,
        isPro = true,
        managementUrl = null,
    )

    @Test
    fun content_lightMode() = snapshot(loadedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun monthlySelected_lightMode() = snapshot(monthlySelectedState, darkTheme = false)

    @Test
    fun monthlySelected_darkMode() = snapshot(monthlySelectedState, darkTheme = true)

    @Test
    fun loading_lightMode() = snapshot(loadingState, darkTheme = false)

    @Test
    fun loading_darkMode() = snapshot(loadingState, darkTheme = true)

    @Test
    fun emptyState_lightMode() = snapshot(emptyState, darkTheme = false)

    @Test
    fun emptyState_darkMode() = snapshot(emptyState, darkTheme = true)

    @Test
    fun errorNetwork_lightMode() = snapshot(errorNetworkState, darkTheme = false)

    @Test
    fun errorNetwork_darkMode() = snapshot(errorNetworkState, darkTheme = true)

    @Test
    fun purchaseInProgress_lightMode() = snapshot(purchaseInProgressState, darkTheme = false)

    @Test
    fun purchaseInProgress_darkMode() = snapshot(purchaseInProgressState, darkTheme = true)

    @Test
    fun restoreInProgress_lightMode() = snapshot(restoreInProgressState, darkTheme = false)

    @Test
    fun restoreInProgress_darkMode() = snapshot(restoreInProgressState, darkTheme = true)

    @Test
    fun singleOffering_lightMode() = snapshot(singleOfferingState, darkTheme = false)

    @Test
    fun singleOffering_darkMode() = snapshot(singleOfferingState, darkTheme = true)

    @Test
    fun proCustomer_lightMode() = snapshot(proCustomerState, darkTheme = false)

    @Test
    fun proCustomer_darkMode() = snapshot(proCustomerState, darkTheme = true)

    @Test
    fun proCustomerManagementUnavailable_lightMode() =
        snapshot(proCustomerManagementUnavailableState, darkTheme = false)

    @Test
    fun proCustomerManagementUnavailable_darkMode() =
        snapshot(proCustomerManagementUnavailableState, darkTheme = true)

    private fun snapshot(state: PaywallState, darkTheme: Boolean) {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = darkTheme) {
                PaywallScreen(
                    state = state,
                    onBack = {},
                )
            }
        }
    }
}
