package com.walcker.identity.data.service

import com.walcker.identity.api.AccountDeletionOutcome
import com.walcker.identity.api.ReauthenticationMethod
import com.walcker.identity.api.ReauthenticationOutcome
import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeAccountDeletionRepository
import com.walcker.identity.fake.FakeAuthRepository
import com.walcker.identity.fake.FakeBillingClient
import com.walcker.identity.fake.FakeProStateCache
import com.walcker.identity.features.data.service.AccountDeletionServiceImpl
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCaseImpl
import com.walcker.identity.features.domain.usecase.RequiresRecentLoginException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AccountDeletionServiceImplTest {
    private val session = UserSession(uid = "uid-1", email = "user@match.app", displayName = "Match User")

    private fun buildService(
        remoteResult: Result<Unit> = Result.success(Unit),
        authRepository: FakeAuthRepository = FakeAuthRepository(initialUser = session),
    ) = AccountDeletionServiceImpl(
        deleteAccountUseCase =
            DeleteAccountUseCaseImpl(
                accountDeletionRepository = FakeAccountDeletionRepository(result = remoteResult),
                authRepository = authRepository,
                billingClient = FakeBillingClient(),
                proStateCache = FakeProStateCache(),
            ),
        authRepository = authRepository,
    )

    private fun oldLoginWith(provider: String?): Pair<AccountDeletionServiceImpl, FakeAuthRepository> {
        val authRepository = FakeAuthRepository(initialUser = session).apply { signInProviderResult = Result.success(provider) }
        return buildService(remoteResult = Result.failure(RequiresRecentLoginException()), authRepository = authRepository) to authRepository
    }

    @Test
    fun `a recent login deletes the account right away`() =
        runTest {
            assertEquals(AccountDeletionOutcome.Success, buildService().deleteAccount())
        }

    @Test
    fun `an old password login asks for the password`() =
        runTest {
            val (service, _) = oldLoginWith("password")

            assertEquals(AccountDeletionOutcome.RequiresReauthentication(ReauthenticationMethod.Password), service.deleteAccount())
        }

    @Test
    fun `an old google login asks for google`() =
        runTest {
            val (service, _) = oldLoginWith("google.com")

            assertEquals(AccountDeletionOutcome.RequiresReauthentication(ReauthenticationMethod.Google), service.deleteAccount())
        }

    @Test
    fun `an old apple login asks for apple`() =
        runTest {
            val (service, _) = oldLoginWith("apple.com")

            assertEquals(AccountDeletionOutcome.RequiresReauthentication(ReauthenticationMethod.Apple), service.deleteAccount())
        }

    @Test
    fun `an old login with an unsupported provider fails instead of asking for something impossible`() =
        runTest {
            val (service, _) = oldLoginWith("phone")

            assertIs<AccountDeletionOutcome.Failure>(service.deleteAccount())
        }

    @Test
    fun `the right password confirms the identity`() =
        runTest {
            val authRepository = FakeAuthRepository(initialUser = session)
            val service = buildService(authRepository = authRepository)

            assertEquals(ReauthenticationOutcome.Success, service.reauthenticateWithPassword("s3cret"))
            assertEquals("s3cret", authRepository.lastReauthenticationPassword)
        }

    @Test
    fun `a wrong password is told apart from other failures`() =
        runTest {
            val authRepository =
                FakeAuthRepository(initialUser = session).apply {
                    reauthenticateWithPasswordResult = Result.failure(IdentityError.InvalidCredentials)
                }

            assertEquals(ReauthenticationOutcome.WrongPassword, buildService(authRepository = authRepository).reauthenticateWithPassword("nope"))
        }

    @Test
    fun `a failed password confirmation for another reason is a failure`() =
        runTest {
            val authRepository =
                FakeAuthRepository(initialUser = session).apply {
                    reauthenticateWithPasswordResult = Result.failure(IdentityError.Network)
                }

            assertIs<ReauthenticationOutcome.Failure>(buildService(authRepository = authRepository).reauthenticateWithPassword("s3cret"))
        }

    @Test
    fun `a google account confirms with google`() =
        runTest {
            val authRepository = FakeAuthRepository(initialUser = session).apply { signInProviderResult = Result.success("google.com") }

            assertEquals(ReauthenticationOutcome.Success, buildService(authRepository = authRepository).reauthenticateWithProvider())
            assertEquals(1, authRepository.reauthenticateWithGoogleCallCount)
            assertEquals(0, authRepository.reauthenticateWithAppleCallCount)
        }

    @Test
    fun `an apple account confirms with apple`() =
        runTest {
            val authRepository = FakeAuthRepository(initialUser = session).apply { signInProviderResult = Result.success("apple.com") }

            assertEquals(ReauthenticationOutcome.Success, buildService(authRepository = authRepository).reauthenticateWithProvider())
            assertEquals(1, authRepository.reauthenticateWithAppleCallCount)
            assertEquals(0, authRepository.reauthenticateWithGoogleCallCount)
        }

    @Test
    fun `closing the provider sheet is a cancellation`() =
        runTest {
            val authRepository =
                FakeAuthRepository(initialUser = session).apply {
                    signInProviderResult = Result.success("google.com")
                    reauthenticateWithGoogleResult = Result.failure(IdentityError.Cancelled)
                }

            assertEquals(ReauthenticationOutcome.Cancelled, buildService(authRepository = authRepository).reauthenticateWithProvider())
        }

    @Test
    fun `a password account cannot confirm through a provider`() =
        runTest {
            val authRepository = FakeAuthRepository(initialUser = session).apply { signInProviderResult = Result.success("password") }

            assertIs<ReauthenticationOutcome.Failure>(buildService(authRepository = authRepository).reauthenticateWithProvider())
            assertEquals(0, authRepository.reauthenticateWithGoogleCallCount)
            assertEquals(0, authRepository.reauthenticateWithAppleCallCount)
        }
}
