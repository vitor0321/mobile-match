package com.walcker.identity.data.verification

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeCrashReporter
import com.walcker.identity.fake.FakeSessionHolder
import com.walcker.identity.fake.FakeVerificationSyncCallableSource
import com.walcker.identity.features.data.verification.VerificationSyncer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationSyncerTest {
    private val unverified = UserSession(uid = "uid-1", email = "ana@match.app", displayName = "Ana")
    private val emailVerified = unverified.copy(isEmailVerified = true)
    private val fullyVerified = emailVerified.copy(phoneNumber = "+5511912345678")

    @Test
    fun `a fully unverified session is not synced`() =
        runTest {
            val source = FakeVerificationSyncCallableSource()
            val syncer = VerificationSyncer(FakeSessionHolder(unverified), source, FakeCrashReporter())

            val job = launch { syncer.start() }
            advanceUntilIdle()
            job.cancel()

            assertEquals(0, source.callCount)
        }

    @Test
    fun `a phone-only verified session is synced`() =
        runTest {
            val source = FakeVerificationSyncCallableSource()
            val phoneOnly = unverified.copy(phoneNumber = "+5511912345678")
            val syncer = VerificationSyncer(FakeSessionHolder(phoneOnly), source, FakeCrashReporter())

            val job = launch { syncer.start() }
            advanceUntilIdle()
            job.cancel()

            assertEquals(1, source.callCount)
        }

    @Test
    fun `each verified step syncs once`() =
        runTest {
            val sessionHolder = FakeSessionHolder(emailVerified)
            val source = FakeVerificationSyncCallableSource()
            val syncer = VerificationSyncer(sessionHolder, source, FakeCrashReporter())

            val job = launch { syncer.start() }
            advanceUntilIdle()
            assertEquals(1, source.callCount)

            sessionHolder.updateUser(emailVerified.copy(displayName = "Ana Souza"))
            advanceUntilIdle()
            assertEquals(1, source.callCount)

            sessionHolder.updateUser(fullyVerified)
            advanceUntilIdle()
            assertEquals(2, source.callCount)
            job.cancel()
        }

    @Test
    fun `a state already synced is skipped when the shell starts again`() =
        runTest {
            val source = FakeVerificationSyncCallableSource()
            val syncer = VerificationSyncer(FakeSessionHolder(fullyVerified), source, FakeCrashReporter())

            val first = launch { syncer.start() }
            advanceUntilIdle()
            first.cancel()
            val second = launch { syncer.start() }
            advanceUntilIdle()
            second.cancel()

            assertEquals(1, source.callCount)
        }

    @Test
    fun `a failed sync is reported and retried on the next start`() =
        runTest {
            val source = FakeVerificationSyncCallableSource(result = Result.failure(IllegalStateException("offline")))
            val crashReporter = FakeCrashReporter()
            val syncer = VerificationSyncer(FakeSessionHolder(fullyVerified), source, crashReporter)

            val first = launch { syncer.start() }
            advanceUntilIdle()
            first.cancel()
            source.result = Result.success(Unit)
            val second = launch { syncer.start() }
            advanceUntilIdle()
            second.cancel()

            assertEquals(2, source.callCount)
            assertEquals(1, crashReporter.recordedExceptions.size)
        }

    @Test
    fun `another account on the same device syncs its own state`() =
        runTest {
            val sessionHolder = FakeSessionHolder(fullyVerified)
            val source = FakeVerificationSyncCallableSource()
            val syncer = VerificationSyncer(sessionHolder, source, FakeCrashReporter())

            val job = launch { syncer.start() }
            advanceUntilIdle()
            sessionHolder.updateUser(fullyVerified.copy(uid = "uid-2"))
            advanceUntilIdle()
            job.cancel()

            assertEquals(2, source.callCount)
        }
}
