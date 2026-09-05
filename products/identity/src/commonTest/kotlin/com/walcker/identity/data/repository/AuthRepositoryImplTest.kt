package com.walcker.identity.data.repository

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeAppleAuthSource
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakeGoogleAuthSource
import com.walcker.identity.features.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryImplTest {
    private val firebaseAuth = FakeFirebaseAuthSource()
    private val googleAuth = FakeGoogleAuthSource()
    private val appleAuth = FakeAppleAuthSource()
    private val repository =
        AuthRepositoryImpl(
            firebaseAuthSource = firebaseAuth,
            googleAuthSource = googleAuth,
            appleAuthSource = appleAuth,
        )

    private val userSession =
        UserSession(
            uid = "uid-123",
            email = "user@match.app",
            displayName = "Match User",
        )

    @Test
    fun `When observing currentUser should delegate to firebaseAuth`() =
        runTest {
            firebaseAuth.emitCurrentUser(userSession)
            val result = repository.currentUser.first()
            assertEquals(userSession, result)
        }

    @Test
    fun `When signIn should delegate to firebaseAuth and return result`() =
        runTest {
            firebaseAuth.setSignInResult(Result.success(userSession))
            val result = repository.signIn("user@match.app", "password123")

            assertEquals(Result.success(userSession), result)
            assertEquals("user@match.app" to "password123", firebaseAuth.lastSignInInput)
        }

    @Test
    fun `When signInWithGoogle should delegate to googleAuth and return result`() =
        runTest {
            googleAuth.setSignInResult(Result.success(userSession))
            val result = repository.signInWithGoogle()

            assertEquals(Result.success(userSession), result)
            assertEquals(1, googleAuth.signInCallCount)
        }

    @Test
    fun `When signInWithApple should delegate to appleAuth and return result`() =
        runTest {
            appleAuth.setSignInResult(Result.success(userSession))
            val result = repository.signInWithApple()

            assertEquals(Result.success(userSession), result)
            assertEquals(1, appleAuth.signInCallCount)
        }

    @Test
    fun `When signUp should delegate to firebaseAuth and return result`() =
        runTest {
            firebaseAuth.setSignUpResult(Result.success(userSession))
            val result = repository.signUp("user@match.app", "password123", "Match User")

            assertEquals(Result.success(userSession), result)
            assertEquals("user@match.app" to "password123", firebaseAuth.lastSignUpInput)
            assertEquals("Match User", firebaseAuth.lastSignUpDisplayName)
        }

    @Test
    fun `When deleteAccount should delegate to firebaseAuth and return result`() =
        runTest {
            firebaseAuth.setDeleteCurrentUserResult(Result.success(Unit))
            val result = repository.deleteAccount()

            assertTrue(result.isSuccess)
            assertEquals(1, firebaseAuth.deleteCurrentUserCallCount)
        }

    @Test
    fun `When signOut should delegate to firebaseAuth and return result`() =
        runTest {
            firebaseAuth.setSignOutResult(Result.success(Unit))
            val result = repository.signOut()

            assertTrue(result.isSuccess)
            assertEquals(1, firebaseAuth.signOutCallCount)
        }

    @Test
    fun `When sendPasswordResetEmail should delegate to firebaseAuth and return result`() =
        runTest {
            firebaseAuth.setSendPasswordResetEmailResult(Result.success(Unit))
            val result = repository.sendPasswordResetEmail("user@match.app")

            assertTrue(result.isSuccess)
            assertEquals("user@match.app", firebaseAuth.lastSendPasswordResetEmailInput)
        }
}
