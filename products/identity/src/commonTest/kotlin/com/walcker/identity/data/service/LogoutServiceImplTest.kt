package com.walcker.identity.data.service

import com.walcker.identity.api.SignOutCleanup
import com.walcker.identity.fake.FakeAuthRepository
import com.walcker.identity.features.data.service.LogoutServiceImpl
import com.walcker.identity.features.data.usecase.SignUseCaseImpl
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogoutServiceImplTest {
    private val authRepository = FakeAuthRepository()
    private val events = mutableListOf<String>()

    private fun service(vararg cleanups: SignOutCleanup) =
        LogoutServiceImpl(
            signUseCase = SignUseCaseImpl(authRepository = authRepository),
            cleanups = cleanups.toList(),
        )

    @Test
    fun `cleanups run while the user is still signed in`() =
        runTest {
            val service =
                service(
                    SignOutCleanup { events += "cleanup, signed out ${authRepository.signOutCallCount} times" },
                )

            service.logout()

            assertEquals(listOf("cleanup, signed out 0 times"), events)
            assertEquals(1, authRepository.signOutCallCount)
        }

    @Test
    fun `every cleanup runs`() =
        runTest {
            service(SignOutCleanup { events += "a" }, SignOutCleanup { events += "b" }).logout()

            assertEquals(listOf("a", "b"), events)
        }

    @Test
    fun `a cleanup that never finishes does not keep the user signed in`() =
        runTest {
            val result = service(SignOutCleanup { awaitCancellation() }).logout()

            assertTrue(result.isSuccess)
            assertEquals(1, authRepository.signOutCallCount)
        }

    @Test
    fun `logout without cleanups just signs out`() =
        runTest {
            assertTrue(service().logout().isSuccess)
            assertEquals(1, authRepository.signOutCallCount)
        }
}
