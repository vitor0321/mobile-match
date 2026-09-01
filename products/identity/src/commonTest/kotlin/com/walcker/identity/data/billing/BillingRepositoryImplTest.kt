package com.walcker.identity.data.billing

import com.walcker.identity.fake.FakeBillingClient
import com.walcker.identity.features.data.billing.BillingRepositoryImpl
import com.walcker.identity.features.domain.billing.ProductOffering
import com.walcker.identity.features.domain.billing.PurchaseError
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BillingRepositoryImplTest {
    private val offering =
        ProductOffering(
            id = "default:monthly",
            offeringId = "default",
            packageId = "monthly",
            title = "Plano mensal",
            description = "Acesso completo",
            priceLabel = "R$ 14,90",
        )

    @Test
    fun `When listing offerings should return immutable offerings from billing client`() =
        runTest {
            val repository =
                BillingRepositoryImpl(
                    billingClient =
                        FakeBillingClient(
                            offeringsResult = Result.success(persistentListOf(offering)),
                        ),
                )

            val result = repository.listOfferings()

            assertEquals(persistentListOf(offering), result.getOrNull())
        }

    @Test
    fun `When purchase succeeds should return Unit then delegate package id to billing client`() =
        runTest {
            val billingClient = FakeBillingClient(purchaseResult = Result.success(true))
            val repository = BillingRepositoryImpl(billingClient = billingClient)

            val result = repository.purchase("monthly")

            assertTrue(result.isSuccess)
            assertEquals(listOf("monthly"), billingClient.purchasedPackageIds)
        }

    @Test
    fun `When restore returns pro access should propagate true then delegate to billing client`() =
        runTest {
            val billingClient = FakeBillingClient(restoreResult = Result.success(true))
            val repository = BillingRepositoryImpl(billingClient = billingClient)

            val result = repository.restore()

            assertEquals(true, result.getOrNull())
            assertEquals(1, billingClient.restoreCallCount)
        }

    @Test
    fun `When restore returns no pro access should propagate false`() =
        runTest {
            val billingClient = FakeBillingClient(restoreResult = Result.success(false))
            val repository = BillingRepositoryImpl(billingClient = billingClient)

            val result = repository.restore()

            assertEquals(false, result.getOrNull())
            assertEquals(1, billingClient.restoreCallCount)
        }

    @Test
    fun `When management url is requested should delegate to billing client then return same value`() =
        runTest {
            val billingClient =
                FakeBillingClient(
                    managementUrlResult = Result.success("https://play.google.com/store/account/subscriptions"),
                )
            val repository = BillingRepositoryImpl(billingClient = billingClient)

            val result = repository.managementUrl()

            assertEquals("https://play.google.com/store/account/subscriptions", result.getOrNull())
            assertEquals(1, billingClient.managementUrlCallCount)
        }

    @Test
    fun `When billing client fails with purchase error should keep mapped failure then repository exposes same domain error`() =
        runTest {
            val repository =
                BillingRepositoryImpl(
                    billingClient =
                        FakeBillingClient(
                            purchaseResult = Result.failure(PurchaseError.Network),
                        ),
                )

            val result = repository.purchase("monthly")

            assertTrue(result.isFailure)
            assertIs<PurchaseError.Network>(result.exceptionOrNull())
        }
}
