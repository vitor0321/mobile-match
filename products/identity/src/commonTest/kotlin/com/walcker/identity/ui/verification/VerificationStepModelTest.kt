package com.walcker.identity.ui.verification

import com.walcker.identity.api.UserSession
import com.walcker.identity.api.VerificationStage
import com.walcker.identity.fake.FakeCrashReporter
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakePhoneAuthSource
import com.walcker.identity.features.data.repository.VerificationRepositoryImpl
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.ui.verification.VerificationStepModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val unverified = UserSession(uid = "uid-1", email = "ana@match.app", displayName = "Ana")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(
        firebase: FakeFirebaseAuthSource,
        crashReporter: FakeCrashReporter = FakeCrashReporter(),
    ) = VerificationStepModel(
        verificationRepository = VerificationRepositoryImpl(firebaseAuthSource = firebase, phoneAuthSource = FakePhoneAuthSource()),
        crashReporter = crashReporter,
    )

    @Test
    fun `stage follows the session`() =
        runTest(testDispatcher) {
            val firebase = FakeFirebaseAuthSource(initialUser = unverified)
            val model = buildModel(firebase)
            advanceUntilIdle()
            assertEquals(VerificationStage.Email, model.state.value)

            firebase.emitCurrentUser(unverified.copy(isEmailVerified = true))
            advanceUntilIdle()
            assertEquals(VerificationStage.Phone, model.state.value)

            firebase.emitCurrentUser(unverified.copy(isEmailVerified = true, phoneNumber = "+5511912345678"))
            advanceUntilIdle()
            assertEquals(VerificationStage.Done, model.state.value)
        }

    @Test
    fun `signing out clears the stage`() =
        runTest(testDispatcher) {
            val firebase = FakeFirebaseAuthSource(initialUser = unverified)
            val model = buildModel(firebase)
            advanceUntilIdle()
            assertEquals(VerificationStage.Email, model.state.value)

            firebase.emitCurrentUser(null)
            advanceUntilIdle()

            assertNull(model.state.value)
        }

    @Test
    fun `resuming refreshes the session`() =
        runTest(testDispatcher) {
            val firebase = FakeFirebaseAuthSource(initialUser = unverified)
            val model = buildModel(firebase)
            advanceUntilIdle()

            model.onResumed()
            advanceUntilIdle()

            assertEquals(1, firebase.refreshSessionCallCount)
        }

    @Test
    fun `a failed refresh is reported and keeps the current stage`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = unverified).apply {
                    refreshSessionResult = Result.failure(VerificationError.Network)
                }
            val crashReporter = FakeCrashReporter()
            val model = buildModel(firebase, crashReporter)
            advanceUntilIdle()

            model.onResumed()
            advanceUntilIdle()

            assertEquals(VerificationStage.Email, model.state.value)
            assertEquals(listOf<Throwable>(VerificationError.Network), crashReporter.recordedExceptions)
        }
}
