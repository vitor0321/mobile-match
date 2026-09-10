package com.walcker.identity.domain.usecase

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeAccountDeletionRepository
import com.walcker.identity.fake.FakeAuthRepository
import com.walcker.identity.fake.FakeBillingClient
import com.walcker.identity.fake.FakeProStateCache
import com.walcker.identity.features.domain.usecase.DeleteAccountResult
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCaseImpl
import com.walcker.identity.features.domain.usecase.RequiresRecentLoginException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeleteAccountUseCaseTest {
    private val session = UserSession(uid = "uid-1", email = "user@match.app", displayName = "Match User")

    private fun buildUseCase(
        accountDeletionRepository: FakeAccountDeletionRepository = FakeAccountDeletionRepository(),
        authRepository: FakeAuthRepository = FakeAuthRepository(initialUser = session),
        billingClient: FakeBillingClient = FakeBillingClient(),
        proStateCache: FakeProStateCache = FakeProStateCache(),
    ) = DeleteAccountUseCaseImpl(
        accountDeletionRepository = accountDeletionRepository,
        authRepository = authRepository,
        billingClient = billingClient,
        proStateCache = proStateCache,
    ) to Fakes(accountDeletionRepository, authRepository, billingClient, proStateCache)

    private data class Fakes(
        val accountDeletionRepository: FakeAccountDeletionRepository,
        val authRepository: FakeAuthRepository,
        val billingClient: FakeBillingClient,
        val proStateCache: FakeProStateCache,
    )

    @Test
    fun `every step succeeds and clears the local pro state for the signed-in user`() =
        runTest {
            val (useCase, fakes) = buildUseCase()

            val result = useCase()

            assertEquals(DeleteAccountResult.Success, result)
            assertEquals(1, fakes.accountDeletionRepository.callCount)
            assertEquals(1, fakes.authRepository.deleteAccountCallCount)
            assertEquals(1, fakes.billingClient.logOutCallCount)
            assertEquals(listOf("uid-1"), fakes.proStateCache.clearedUids)
        }

    @Test
    fun `a RequiresRecentLoginException from deleteRemoteData stops the flow immediately`() =
        runTest {
            val accountDeletionRepository = FakeAccountDeletionRepository(result = Result.failure(RequiresRecentLoginException()))
            val (useCase, fakes) = buildUseCase(accountDeletionRepository = accountDeletionRepository)

            val result = useCase()

            assertEquals(DeleteAccountResult.RequiresRecentLogin, result)
            assertEquals(0, fakes.authRepository.deleteAccountCallCount)
            assertEquals(0, fakes.billingClient.logOutCallCount)
            assertTrue(fakes.proStateCache.clearedUids.isEmpty())
        }

    @Test
    fun `a generic failure from deleteRemoteData surfaces as RemoteDataFailure and stops the flow`() =
        runTest {
            val cause = IllegalStateException("network down")
            val accountDeletionRepository = FakeAccountDeletionRepository(result = Result.failure(cause))
            val (useCase, fakes) = buildUseCase(accountDeletionRepository = accountDeletionRepository)

            val result = useCase()

            val failure = assertIs<DeleteAccountResult.RemoteDataFailure>(result)
            assertEquals(cause, failure.cause)
            assertEquals(0, fakes.authRepository.deleteAccountCallCount)
            assertEquals(0, fakes.billingClient.logOutCallCount)
        }

    @Test
    fun `no authenticated user stops the flow before touching auth deletion`() =
        runTest {
            val authRepository = FakeAuthRepository(initialUser = null)
            val (useCase, fakes) = buildUseCase(authRepository = authRepository)

            val result = useCase()

            val failure = assertIs<DeleteAccountResult.AuthDeletionFailure>(result)
            assertIs<IllegalStateException>(failure.cause)
            assertEquals(0, fakes.authRepository.deleteAccountCallCount)
            assertEquals(0, fakes.billingClient.logOutCallCount)
        }

    @Test
    fun `a RequiresRecentLoginException from deleteAccount stops before billing cleanup`() =
        runTest {
            val authRepository =
                FakeAuthRepository(initialUser = session, deleteAccountResult = Result.failure(RequiresRecentLoginException()))
            val (useCase, fakes) = buildUseCase(authRepository = authRepository)

            val result = useCase()

            assertEquals(DeleteAccountResult.RequiresRecentLogin, result)
            assertEquals(0, fakes.billingClient.logOutCallCount)
            assertTrue(fakes.proStateCache.clearedUids.isEmpty())
        }

    @Test
    fun `a generic failure from deleteAccount surfaces as AuthDeletionFailure and stops before billing cleanup`() =
        runTest {
            val cause = IllegalStateException("auth service unavailable")
            val authRepository = FakeAuthRepository(initialUser = session, deleteAccountResult = Result.failure(cause))
            val (useCase, fakes) = buildUseCase(authRepository = authRepository)

            val result = useCase()

            val failure = assertIs<DeleteAccountResult.AuthDeletionFailure>(result)
            assertEquals(cause, failure.cause)
            assertEquals(0, fakes.billingClient.logOutCallCount)
        }

    @Test
    fun `a failure logging out of billing surfaces as LocalCleanupFailure and skips the pro state clear`() =
        runTest {
            val cause = IllegalStateException("billing SDK error")
            val billingClient = FakeBillingClient(logOutResult = Result.failure(cause))
            val (useCase, fakes) = buildUseCase(billingClient = billingClient)

            val result = useCase()

            val failure = assertIs<DeleteAccountResult.LocalCleanupFailure>(result)
            assertEquals(cause, failure.cause)
            assertTrue(fakes.proStateCache.clearedUids.isEmpty())
        }

    @Test
    fun `a failure clearing the local pro state surfaces as LocalCleanupFailure`() =
        runTest {
            val cause = IllegalStateException("disk full")
            val proStateCache = FakeProStateCache().apply { clearResult = Result.failure(cause) }
            val (useCase, _) = buildUseCase(proStateCache = proStateCache)

            val result = useCase()

            val failure = assertIs<DeleteAccountResult.LocalCleanupFailure>(result)
            assertEquals(cause, failure.cause)
        }
}
