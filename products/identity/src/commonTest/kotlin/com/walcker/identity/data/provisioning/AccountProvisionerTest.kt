package com.walcker.identity.data.provisioning

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeCrashReporter
import com.walcker.identity.fake.FakeFirestoreClient
import com.walcker.identity.fake.FakeSessionHolder
import com.walcker.identity.features.data.provisioning.AccountProvisioner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AccountProvisionerTest {
    private val ana = UserSession(uid = "ana", email = "ana@match.app", displayName = "Ana")
    private val firestore = FakeFirestoreClient()
    private val crashReporter = FakeCrashReporter()

    private fun TestScope.run(provisioner: AccountProvisioner) {
        val job = launch { provisioner.start() }
        advanceUntilIdle()
        job.cancel()
    }

    @Test
    fun `a signed in account is provisioned`() =
        runTest {
            run(AccountProvisioner(FakeSessionHolder(ana), firestore, crashReporter))

            assertEquals(listOf("ensureUserProvisioned"), firestore.functionCalls)
        }

    @Test
    fun `nobody signed in means nothing to provision`() =
        runTest {
            run(AccountProvisioner(FakeSessionHolder(null), firestore, crashReporter))

            assertEquals(emptyList(), firestore.functionCalls)
        }

    @Test
    fun `the same account is provisioned only once`() =
        runTest {
            val sessionHolder = FakeSessionHolder(ana)
            val provisioner = AccountProvisioner(sessionHolder, firestore, crashReporter)
            val job = launch { provisioner.start() }
            advanceUntilIdle()

            sessionHolder.updateUser(ana.copy(isEmailVerified = true))
            advanceUntilIdle()
            job.cancel()
            run(provisioner)

            assertEquals(1, firestore.functionCalls.size)
        }

    @Test
    fun `another account on the same phone is provisioned too`() =
        runTest {
            val sessionHolder = FakeSessionHolder(ana)
            val job = launch { AccountProvisioner(sessionHolder, firestore, crashReporter).start() }
            advanceUntilIdle()

            sessionHolder.updateUser(null)
            advanceUntilIdle()
            sessionHolder.updateUser(UserSession(uid = "bia", email = null, displayName = null))
            advanceUntilIdle()
            job.cancel()

            assertEquals(2, firestore.functionCalls.size)
        }

    @Test
    fun `a failure is reported and tried again on the next start`() =
        runTest {
            val provisioner = AccountProvisioner(FakeSessionHolder(ana), firestore, crashReporter)
            firestore.functionResult = Result.failure(IllegalStateException("offline"))
            run(provisioner)

            firestore.functionResult = Result.success(mapOf("provisioned" to true))
            run(provisioner)

            assertEquals(1, crashReporter.recordedExceptions.size)
            assertEquals(2, firestore.functionCalls.size)
        }
}
