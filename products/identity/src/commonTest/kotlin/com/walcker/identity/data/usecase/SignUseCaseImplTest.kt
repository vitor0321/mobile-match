package com.walcker.identity.data.usecase

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeAuthRepository
import com.walcker.identity.features.data.usecase.SignUseCaseImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignUseCaseImplTest {
    private val authRepository = FakeAuthRepository()
    private val useCase = SignUseCaseImpl(authRepository = authRepository)

    private val userSession =
        UserSession(
            uid = "uid-123",
            email = "user@match.app",
            displayName = "Match User",
        )

    @Test
    fun `When observeSession should delegate to authRepository`() =
        runTest {
            authRepository.emitCurrentUser(userSession)
            val result = useCase.observeSession().first()
            assertEquals(userSession, result)
        }

    @Test
    fun `When signInWithEmail should delegate to authRepository and return result`() =
        runTest {
            authRepository.setSignInResult(Result.success(userSession))
            val result = useCase.signInWithEmail("user@match.app", "password123")

            assertEquals(Result.success(userSession), result)
            assertEquals("user@match.app" to "password123", authRepository.lastSignInInput)
        }

    @Test
    fun `When signInWithGoogle should delegate to authRepository and return result`() =
        runTest {
            authRepository.setSignInWithGoogleResult(Result.success(userSession))
            val result = useCase.signInWithGoogle()

            assertEquals(Result.success(userSession), result)
            assertEquals(1, authRepository.signInWithGoogleCallCount)
        }

    @Test
    fun `When signInWithApple should delegate to authRepository and return result`() =
        runTest {
            authRepository.setSignInWithAppleResult(Result.success(userSession))
            val result = useCase.signInWithApple()

            assertEquals(Result.success(userSession), result)
            assertEquals(1, authRepository.signInWithAppleCallCount)
        }

    @Test
    fun `When signUp should delegate to authRepository and return result`() =
        runTest {
            authRepository.setSignUpResult(Result.success(userSession))
            val result = useCase.signUp("user@match.app", "password123", "Match User")

            assertEquals(Result.success(userSession), result)
            assertEquals("user@match.app" to "password123", authRepository.lastSignUpInput)
            assertEquals("Match User", authRepository.lastSignUpDisplayName)
        }

    @Test
    fun `When deleteAccount should delegate to authRepository and return result`() =
        runTest {
            authRepository.setDeleteAccountResult(Result.success(Unit))
            val result = useCase.deleteAccount()

            assertTrue(result.isSuccess)
            assertEquals(1, authRepository.deleteAccountCallCount)
        }

    @Test
    fun `When signOut should delegate to authRepository and return result`() =
        runTest {
            authRepository.setSignOutResult(Result.success(Unit))
            val result = useCase.signOut()

            assertTrue(result.isSuccess)
            assertEquals(1, authRepository.signOutCallCount)
        }

    @Test
    fun `When sendPasswordResetEmail should delegate to authRepository and return result`() =
        runTest {
            authRepository.setSendPasswordResetEmailResult(Result.success(Unit))
            val result = useCase.sendPasswordResetEmail("user@match.app")

            assertTrue(result.isSuccess)
            assertEquals("user@match.app", authRepository.lastSendPasswordResetEmailInput)
        }
}
