package com.walcker.identity.domain.usecase

import com.walcker.identity.fake.FakeBillingRepository
import com.walcker.identity.fake.FakeProStateHolder
import com.walcker.identity.features.domain.usecase.ProfileAccountUseCaseImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileAccountUseCaseTest {
    @Test
    fun `a pro user's subscription state includes the management url`() =
        runTest {
            val proStateHolder = FakeProStateHolder(initialIsPro = true)
            val billingRepository = FakeBillingRepository(managementUrlResult = Result.success("https://manage.example.com"))
            val useCase = ProfileAccountUseCaseImpl(proStateHolder, billingRepository)

            val state = useCase.observeSubscription().first()

            assertTrue(state.isPro)
            assertEquals("https://manage.example.com", state.managementUrl)
            assertEquals(1, billingRepository.managementUrlCallCount)
        }

    @Test
    fun `a non-pro user's subscription state has no management url and skips the lookup`() =
        runTest {
            val proStateHolder = FakeProStateHolder(initialIsPro = false)
            val billingRepository = FakeBillingRepository()
            val useCase = ProfileAccountUseCaseImpl(proStateHolder, billingRepository)

            val state = useCase.observeSubscription().first()

            assertFalse(state.isPro)
            assertNull(state.managementUrl)
            assertEquals(0, billingRepository.managementUrlCallCount)
        }

    @Test
    fun `a failed management url lookup surfaces no url instead of throwing`() =
        runTest {
            val proStateHolder = FakeProStateHolder(initialIsPro = true)
            val billingRepository = FakeBillingRepository(managementUrlResult = Result.failure(IllegalStateException("offline")))
            val useCase = ProfileAccountUseCaseImpl(proStateHolder, billingRepository)

            val state = useCase.observeSubscription().first()

            assertTrue(state.isPro)
            assertNull(state.managementUrl)
        }

    @Test
    fun `restorePurchases delegates to the billing repository`() =
        runTest {
            val billingRepository = FakeBillingRepository(restoreResult = Result.success(true))
            val useCase = ProfileAccountUseCaseImpl(FakeProStateHolder(), billingRepository)

            val result = useCase.restorePurchases()

            assertEquals(Result.success(true), result)
            assertEquals(1, billingRepository.restoreCallCount)
        }
}
