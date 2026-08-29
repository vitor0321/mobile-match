package com.walcker.identity.data.pro

import app.cash.turbine.test
import com.walcker.identity.api.TrialStatus
import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeBillingClient
import com.walcker.identity.fake.FakeProStateCache
import com.walcker.identity.fake.FakeSessionHolder
import com.walcker.identity.features.data.pro.ProStateHolderImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock.System

@OptIn(ExperimentalCoroutinesApi::class)
class ProStateHolderImplTest {
    private val userSession = UserSession(
        uid = "uid-1",
        email = "user@match.app",
        displayName = "Match User",
    )

    @Test
    fun `When holder starts should hydrate cached state immediately then expose cached Pro value`() = runTest {
        val billingClient = FakeBillingClient(
            logInResult = Result.failure(IllegalStateException("sync error")),
        )
        val holder = ProStateHolderImpl(
            sessionHolder = FakeSessionHolder(initialUser = userSession),
            billingClient = billingClient,
            cache = FakeProStateCache(initialIsPro = true),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        holder.isPro.test {
            assertFalse(awaitItem())
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When session becomes unauthenticated should log out billing then expose false`() = runTest {
        val sessionHolder = FakeSessionHolder(initialUser = userSession)
        val billingClient = FakeBillingClient(logInResult = Result.success(true))
        val cache = FakeProStateCache(initialIsPro = false)
        val holder = ProStateHolderImpl(
            sessionHolder = sessionHolder,
            billingClient = billingClient,
            cache = cache,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        sessionHolder.updateUser(null)
        advanceUntilIdle()

        assertEquals(1, billingClient.logOutCallCount)
        assertFalse(holder.isProState())
        assertEquals(listOf("uid-1" to true), cache.savedValues)
    }

    @Test
    fun `When session becomes authenticated should log in with Firebase uid then keep billing user in sync`() = runTest {
        val sessionHolder = FakeSessionHolder(initialUser = null)
        val billingClient = FakeBillingClient(logInResult = Result.success(false))
        val holder = ProStateHolderImpl(
            sessionHolder = sessionHolder,
            billingClient = billingClient,
            cache = FakeProStateCache(initialIsPro = false),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        sessionHolder.updateUser(userSession)
        advanceUntilIdle()

        assertEquals("uid-1", billingClient.lastLoggedInUserId)
        assertFalse(holder.isProState())
    }

    @Test
    fun `When billing customer info updates should expose new Pro state then save cache`() = runTest {
        val billingClient = FakeBillingClient(logInResult = Result.success(false))
        val cache = FakeProStateCache(initialIsPro = false)
        val holder = ProStateHolderImpl(
            sessionHolder = FakeSessionHolder(initialUser = userSession),
            billingClient = billingClient,
            cache = cache,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        billingClient.emitCustomerInfoUpdate(true)
        advanceUntilIdle()

        assertTrue(holder.isProState())
        assertEquals(listOf("uid-1" to false, "uid-1" to true), cache.savedValues)
    }

    @Test
    fun `When a stale billing update belongs to a previous user should not publish or cache it`() = runTest {
        val secondUser = userSession.copy(uid = "uid-2")
        val sessionHolder = FakeSessionHolder(initialUser = userSession)
        val billingClient = FakeBillingClient(logInResult = Result.success(false))
        val cache = FakeProStateCache(initialIsPro = false)
        val holder = ProStateHolderImpl(
            sessionHolder = sessionHolder,
            billingClient = billingClient,
            cache = cache,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        sessionHolder.updateUser(secondUser)
        advanceUntilIdle()
        billingClient.emitCustomerInfoUpdate(isPro = true, uid = userSession.uid)
        advanceUntilIdle()

        assertFalse(holder.isProState())
        assertEquals(listOf("uid-1" to false, "uid-2" to false), cache.savedValues)
    }

    @Test
    fun `When user is not authenticated checkTrialStatus should return NotAuthenticated`() = runTest {
        val holder = ProStateHolderImpl(
            sessionHolder = FakeSessionHolder(initialUser = null),
            billingClient = FakeBillingClient(),
            cache = FakeProStateCache(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        assertEquals(com.walcker.identity.api.TrialStatus.NotAuthenticated, holder.checkTrialStatus())
    }

    @Test
    fun `When user is authenticated and has active cached registration date checkTrialStatus should return Active`() = runTest {
        val cache = FakeProStateCache()
        val currentDay = com.walcker.identity.features.data.pro.currentDayKey()
        cache.saveRegistrationDate("uid-1", currentDay)

        val holder = ProStateHolderImpl(
            sessionHolder = FakeSessionHolder(initialUser = userSession),
            billingClient = FakeBillingClient(),
            cache = cache,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        assertEquals(TrialStatus.Active, holder.checkTrialStatus())
    }

    @Test
    fun `When user is authenticated and has expired cached registration date checkTrialStatus should return Expired`() = runTest {
        val cache = FakeProStateCache()
        val creationMillis = System.now().toEpochMilliseconds() - 5 * 24 * 60 * 60 * 1000L
        val dateStr = com.walcker.identity.features.data.pro.formatEpochMillisToDayKey(creationMillis)
        cache.saveRegistrationDate("uid-1", dateStr)

        val holder = ProStateHolderImpl(
            sessionHolder = FakeSessionHolder(initialUser = userSession),
            billingClient = FakeBillingClient(),
            cache = cache,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        assertEquals(TrialStatus.Expired, holder.checkTrialStatus())
    }

    @Test
    fun `When user has no cached date but has active Firebase creationTimestamp checkTrialStatus should save to cache and return Active`() = runTest {
        val cache = FakeProStateCache()
        val creationMillis = System.now()
        val userSessionWithCreation = userSession.copy(creationTimestamp = creationMillis.toEpochMilliseconds())

        val holder = ProStateHolderImpl(
            sessionHolder = FakeSessionHolder(initialUser = userSessionWithCreation),
            billingClient = FakeBillingClient(),
            cache = cache,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        assertEquals(com.walcker.identity.api.TrialStatus.Active, holder.checkTrialStatus())
        val expectedDay = com.walcker.identity.features.data.pro.formatEpochMillisToDayKey(creationMillis.toEpochMilliseconds())
        assertEquals(expectedDay, cache.readRegistrationDate("uid-1"))
    }

    @Test
    fun `When user has no cached date and no Firebase creationTimestamp checkTrialStatus should return OfflineAndNoCachedDate`() = runTest {
        val holder = ProStateHolderImpl(
            sessionHolder = FakeSessionHolder(initialUser = userSession),
            billingClient = FakeBillingClient(),
            cache = FakeProStateCache(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        assertEquals(com.walcker.identity.api.TrialStatus.OfflineAndNoCachedDate, holder.checkTrialStatus())
    }

    private suspend fun ProStateHolderImpl.isProState(): Boolean = isPro.first()
}

