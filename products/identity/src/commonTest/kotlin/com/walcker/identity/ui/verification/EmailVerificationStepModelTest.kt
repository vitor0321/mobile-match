package com.walcker.identity.ui.verification

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeCrashReporter
import com.walcker.identity.fake.FakeEmailVerificationThrottle
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakeLogoutService
import com.walcker.identity.fake.FakePhoneAuthSource
import com.walcker.identity.features.data.repository.VerificationRepositoryImpl
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.ui.verification.RESEND_COOLDOWN_SECONDS
import com.walcker.identity.features.ui.verification.email.EmailVerificationInternalRoute
import com.walcker.identity.features.ui.verification.email.EmailVerificationStepModel
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.PtBrIdentityStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = IdentityStringsHolder().apply { setStrings(PtBrIdentityStrings) }
    private val strings = PtBrIdentityStrings.verification
    private val unverified = UserSession(uid = "uid-1", email = "ana@match.app", displayName = "Ana")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private class Fixture(
        val model: EmailVerificationStepModel,
        val firebase: FakeFirebaseAuthSource,
        val throttle: FakeEmailVerificationThrottle,
        val logoutService: FakeLogoutService,
    )

    private fun buildFixture(
        firebase: FakeFirebaseAuthSource = FakeFirebaseAuthSource(initialUser = unverified),
        throttle: FakeEmailVerificationThrottle = FakeEmailVerificationThrottle(),
        logoutService: FakeLogoutService = FakeLogoutService(),
    ): Fixture {
        val model =
            EmailVerificationStepModel(
                verificationRepository = VerificationRepositoryImpl(firebaseAuthSource = firebase, phoneAuthSource = FakePhoneAuthSource()),
                throttle = throttle,
                logoutService = logoutService,
                stringsHolder = stringsHolder,
                crashReporter = FakeCrashReporter(),
            )
        return Fixture(model, firebase, throttle, logoutService)
    }

    @Test
    fun `opening sends the e-mail and starts the resend wait`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            runCurrent()

            val state = fixture.model.state.value
            assertEquals("ana@match.app", state.email)
            assertEquals(1, fixture.firebase.sendEmailVerificationCallCount)
            assertEquals(listOf("uid-1"), fixture.throttle.markedSentUids)
            assertEquals(strings.emailSent, state.message)
            assertFalse(state.isMessageError)
            assertEquals(RESEND_COOLDOWN_SECONDS, state.resendAvailableInSeconds)
        }

    @Test
    fun `opening inside the send window does not send again`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(throttle = FakeEmailVerificationThrottle(shouldAutoSend = false))
            advanceUntilIdle()

            assertEquals(0, fixture.firebase.sendEmailVerificationCallCount)
            assertEquals(0, fixture.model.state.value.resendAvailableInSeconds)
        }

    @Test
    fun `an already verified e-mail is never sent`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(firebase = FakeFirebaseAuthSource(initialUser = unverified.copy(isEmailVerified = true)))
            advanceUntilIdle()

            assertEquals(0, fixture.firebase.sendEmailVerificationCallCount)
        }

    @Test
    fun `a new emission of the same account does not send twice`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.firebase.emitCurrentUser(unverified.copy(displayName = "Ana Souza"))
            advanceUntilIdle()

            assertEquals(1, fixture.firebase.sendEmailVerificationCallCount)
        }

    @Test
    fun `resend is ignored while the wait is running`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            runCurrent()

            fixture.model.onEvent(EmailVerificationInternalRoute.OnResendClicked)
            runCurrent()

            assertEquals(1, fixture.firebase.sendEmailVerificationCallCount)
        }

    @Test
    fun `resend works again once the wait is over`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            runCurrent()

            advanceTimeBy(RESEND_COOLDOWN_SECONDS * 1_000L)
            runCurrent()
            assertEquals(0, fixture.model.state.value.resendAvailableInSeconds)

            fixture.model.onEvent(EmailVerificationInternalRoute.OnResendClicked)
            runCurrent()

            assertEquals(2, fixture.firebase.sendEmailVerificationCallCount)
        }

    @Test
    fun `a failed send shows the mapped error without starting the wait`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = unverified).apply {
                    sendEmailVerificationResult = Result.failure(VerificationError.TooManyRequests)
                }
            val fixture = buildFixture(firebase = firebase)
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(strings.errorTooManyRequests, state.message)
            assertTrue(state.isMessageError)
            assertEquals(0, state.resendAvailableInSeconds)
            assertTrue(fixture.throttle.markedSentUids.isEmpty())
        }

    @Test
    fun `confirming while still unverified explains what is missing`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(throttle = FakeEmailVerificationThrottle(shouldAutoSend = false))
            advanceUntilIdle()

            fixture.model.onEvent(EmailVerificationInternalRoute.OnConfirmedClicked)
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(1, fixture.firebase.refreshSessionCallCount)
            assertEquals(strings.emailNotConfirmedYet, state.message)
            assertTrue(state.isMessageError)
            assertFalse(state.isChecking)
        }

    @Test
    fun `confirming after verifying shows no message`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = unverified).apply {
                    refreshSessionResult = Result.success(unverified.copy(isEmailVerified = true))
                }
            val fixture = buildFixture(firebase = firebase, throttle = FakeEmailVerificationThrottle(shouldAutoSend = false))
            advanceUntilIdle()

            fixture.model.onEvent(EmailVerificationInternalRoute.OnConfirmedClicked)
            advanceUntilIdle()

            assertNull(fixture.model.state.value.message)
            assertFalse(fixture.model.state.value.isChecking)
        }

    @Test
    fun `dismissing clears the message`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(EmailVerificationInternalRoute.OnMessageDismissed)

            assertNull(fixture.model.state.value.message)
        }

    @Test
    fun `logging out calls the logout service`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(throttle = FakeEmailVerificationThrottle(shouldAutoSend = false))
            advanceUntilIdle()

            fixture.model.onEvent(EmailVerificationInternalRoute.OnLogoutClicked)
            advanceUntilIdle()

            assertEquals(1, fixture.logoutService.logoutCallCount)
        }

    @Test
    fun `a failed logout shows a message instead of failing silently`() =
        runTest(testDispatcher) {
            val fixture =
                buildFixture(
                    throttle = FakeEmailVerificationThrottle(shouldAutoSend = false),
                    logoutService = FakeLogoutService(result = Result.failure(IllegalStateException("offline"))),
                )
            advanceUntilIdle()

            fixture.model.onEvent(EmailVerificationInternalRoute.OnLogoutClicked)
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(strings.errorGeneric, state.message)
            assertTrue(state.isMessageError)
        }
}
