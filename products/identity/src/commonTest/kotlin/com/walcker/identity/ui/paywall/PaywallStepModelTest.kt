package com.walcker.identity.ui.paywall

import app.cash.turbine.test
import com.walcker.identity.fake.FakeBillingRepository
import com.walcker.identity.fake.FakeProStateHolder
import com.walcker.identity.fake.FakeSessionHolder
import com.walcker.identity.features.domain.billing.ProductOffering
import com.walcker.identity.features.domain.billing.PurchaseError
import com.walcker.identity.features.ui.paywall.PaywallError
import com.walcker.identity.features.ui.paywall.PaywallInternalEvents
import com.walcker.identity.features.ui.paywall.PaywallInternalRoute
import com.walcker.identity.features.ui.paywall.PaywallOfferingPeriod
import com.walcker.identity.features.ui.paywall.PaywallStepModel
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.PtBrIdentityStrings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = IdentityStringsHolder().apply { setStrings(PtBrIdentityStrings) }

    private val offering =
        ProductOffering(
            id = "default:monthly",
            offeringId = "default",
            packageId = "monthly",
            title = "Plano mensal",
            description = "Acesso completo",
            priceLabel = "R$ 14,90",
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        billingRepository: FakeBillingRepository =
            FakeBillingRepository(
                offeringsResult = Result.success(persistentListOf(offering)),
            ),
        proStateHolder: FakeProStateHolder = FakeProStateHolder(),
    ): Pair<PaywallStepModel, FakeBillingRepository> =
        PaywallStepModel(
            billingRepository = billingRepository,
            sessionHolder = sessionHolder,
            proStateHolder = proStateHolder,
            stringsHolder = stringsHolder,
        ) to billingRepository

    @Test
    fun `When paywall opens without authenticated user should require login then stop loading`() =
        runTest(testDispatcher) {
            val (model, _) = buildModel(sessionHolder = FakeSessionHolder(initialUser = null))

            model.events.test {
                assertEquals(PaywallInternalEvents.RequireLogin, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `When paywall opens authenticated should load offerings then select first plan`() =
        runTest(testDispatcher) {
            val (model, _) = buildModel()
            advanceUntilIdle()

            assertFalse(model.state.value.isLoading)
            assertEquals(persistentListOf(offering), model.state.value.offerings)
            assertEquals(offering.id, model.state.value.selectedOfferingId)
            assertEquals(offering, model.state.value.selectedOffering)
            assertEquals(PaywallOfferingPeriod.MONTHLY, model.state.value.selectedOfferingPeriod)
        }

    @Test
    fun `When purchase succeeds should dismiss paywall then delegate selected package id`() =
        runTest(testDispatcher) {
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering)),
                    purchaseResult = Result.success(Unit),
                )
            val (model, _) = buildModel(billingRepository = repository)
            advanceUntilIdle()

            model.events.test {
                model.onEvent(PaywallInternalRoute.OnPurchaseClicked)
                assertEquals(PaywallInternalEvents.Dismiss, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(listOf("monthly"), repository.purchasedPackageIds)
            assertEquals(null, model.state.value.purchaseInProgress)
        }

    @Test
    fun `When restore returns pro access should show success snackbar then keep paywall open`() =
        runTest(testDispatcher) {
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering)),
                    restoreResult = Result.success(true),
                )
            val (model, _) = buildModel(billingRepository = repository)
            advanceUntilIdle()

            model.events.test {
                model.onEvent(PaywallInternalRoute.OnRestoreClicked)
                assertEquals(
                    PaywallInternalEvents.ShowSnackbar("Compras restauradas."),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, repository.restoreCallCount)
            assertFalse(model.state.value.isRestoring)
        }

    @Test
    fun `When restore returns no pro access should show no purchases snackbar`() =
        runTest(testDispatcher) {
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering)),
                    restoreResult = Result.success(false),
                )
            val (model, _) = buildModel(billingRepository = repository)
            advanceUntilIdle()

            model.events.test {
                model.onEvent(PaywallInternalRoute.OnRestoreClicked)
                assertEquals(
                    PaywallInternalEvents.ShowSnackbar(
                        "Nenhuma compra ativa foi encontrada para restaurar.",
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, repository.restoreCallCount)
            assertFalse(model.state.value.isRestoring)
        }

    @Test
    fun `When pro customer opens paywall should load offerings just like a free user`() =
        runTest(testDispatcher) {
            val annual =
                ProductOffering(
                    id = "default:annual",
                    offeringId = "default",
                    packageId = "annual",
                    title = "Plano anual",
                    description = "Melhor custo-benefício",
                    priceLabel = "R$ 119,90",
                )
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering, annual)),
                )
            val (model, _) = buildModel(billingRepository = repository)

            advanceUntilIdle()

            assertFalse(model.state.value.isLoading)
            assertEquals(persistentListOf(offering, annual), model.state.value.offerings)
            assertEquals(offering.id, model.state.value.selectedOfferingId)
            assertEquals(offering, model.state.value.selectedOffering)
            assertEquals(PaywallOfferingPeriod.MONTHLY, model.state.value.selectedOfferingPeriod)
        }

    @Test
    fun `When offering selection changes should expose selected offering and inferred period on state`() =
        runTest(testDispatcher) {
            val annual =
                ProductOffering(
                    id = "default:annual",
                    offeringId = "default",
                    packageId = "annual",
                    title = "Plano anual",
                    description = "Melhor custo-benefício",
                    priceLabel = "R$ 119,90",
                )
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering, annual)),
                )
            val (model, _) = buildModel(billingRepository = repository)

            advanceUntilIdle()
            model.onEvent(PaywallInternalRoute.OnOfferingSelected(annual.id))

            assertEquals(annual.id, model.state.value.selectedOfferingId)
            assertEquals(annual, model.state.value.selectedOffering)
            assertEquals(PaywallOfferingPeriod.YEARLY, model.state.value.selectedOfferingPeriod)
        }

    @Test
    fun `When pro state turns true should fetch management url then expose it on state`() =
        runTest(testDispatcher) {
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering)),
                    managementUrlResult = Result.success("https://play.google.com/store/account/subscriptions"),
                )
            val proStateHolder = FakeProStateHolder(initialIsPro = false)
            val (model, _) =
                buildModel(
                    billingRepository = repository,
                    proStateHolder = proStateHolder,
                )
            advanceUntilIdle()
            proStateHolder.update(true)
            advanceUntilIdle()

            assertTrue(model.state.value.isPro)
            assertEquals(
                "https://play.google.com/store/account/subscriptions",
                model.state.value.managementUrl,
            )
            assertEquals(1, repository.managementUrlCallCount)
        }

    @Test
    fun `When pro state turns false should clear management url`() =
        runTest(testDispatcher) {
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering)),
                    managementUrlResult = Result.success("https://play.google.com/store/account/subscriptions"),
                )
            val proStateHolder = FakeProStateHolder(initialIsPro = true)
            val (model, _) =
                buildModel(
                    billingRepository = repository,
                    proStateHolder = proStateHolder,
                )
            advanceUntilIdle()
            proStateHolder.update(false)
            advanceUntilIdle()

            assertFalse(model.state.value.isPro)
            assertEquals(null, model.state.value.managementUrl)
        }

    @Test
    fun `When purchase fails should expose domain error then keep screen open`() =
        runTest(testDispatcher) {
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.success(persistentListOf(offering)),
                    purchaseResult = Result.failure(IllegalStateException("falhou")),
                )
            val (model, _) = buildModel(billingRepository = repository)
            advanceUntilIdle()

            model.onEvent(PaywallInternalRoute.OnPurchaseClicked)
            advanceUntilIdle()

            assertTrue(model.state.value.error != null)
            assertEquals(PtBrIdentityStrings.paywall.genericError, model.state.value.errorMessage)
            assertEquals(null, model.state.value.purchaseInProgress)
        }

    @Test
    fun `When offerings load fails should expose localized error message on state`() =
        runTest(testDispatcher) {
            val repository =
                FakeBillingRepository(
                    offeringsResult = Result.failure(PurchaseError.Network),
                )
            val (model, _) = buildModel(billingRepository = repository)

            advanceUntilIdle()

            assertEquals(PaywallError.Network, model.state.value.error)
            assertEquals(PtBrIdentityStrings.paywall.networkError, model.state.value.errorMessage)
        }
}
