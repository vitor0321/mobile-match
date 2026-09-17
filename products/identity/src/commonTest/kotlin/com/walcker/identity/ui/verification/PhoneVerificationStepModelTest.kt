package com.walcker.identity.ui.verification

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeCrashReporter
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakeLogoutService
import com.walcker.identity.fake.FakePhoneAuthSource
import com.walcker.identity.features.data.repository.VerificationRepositoryImpl
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.phone.CountryDialCode
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession
import com.walcker.identity.features.ui.verification.RESEND_COOLDOWN_SECONDS
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationInternalRoute
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationMode
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationPhase
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationStepModel
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneVerificationStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = IdentityStringsHolder().apply { setStrings(PtBrIdentityStrings) }
    private val strings = PtBrIdentityStrings.verification
    private val brazil = CountryDialCode(isoCode = "BR", dialCode = "55")
    private val emailVerified = UserSession(uid = "uid-1", email = "ana@match.app", displayName = "Ana", isEmailVerified = true)
    private val sentSession =
        PhoneVerificationSession(e164 = "+5511912345678", verificationId = "verification-1", platformResendToken = "resend-token")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private class Fixture(
        val model: PhoneVerificationStepModel,
        val firebase: FakeFirebaseAuthSource,
        val phone: FakePhoneAuthSource,
        val logoutService: FakeLogoutService,
    )

    private fun buildFixture(
        firebase: FakeFirebaseAuthSource = FakeFirebaseAuthSource(initialUser = emailVerified),
        phone: FakePhoneAuthSource = FakePhoneAuthSource(sendCodeResult = Result.success(PhoneCodeResult.CodeSent(sentSession))),
        logoutService: FakeLogoutService = FakeLogoutService(),
        mode: PhoneVerificationMode = PhoneVerificationMode.Verify,
        regionIsoCode: String? = null,
    ): Fixture {
        val model =
            PhoneVerificationStepModel(
                verificationRepository = VerificationRepositoryImpl(firebaseAuthSource = firebase, phoneAuthSource = phone),
                logoutService = logoutService,
                stringsHolder = stringsHolder,
                crashReporter = FakeCrashReporter(),
                initialCountry = brazil,
                mode = mode,
                regionIsoCode = regionIsoCode,
            )
        return Fixture(model, firebase, phone, logoutService)
    }

    private fun Fixture.typeValidBrazilianNumberAndSend() {
        model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("(11) 91234-5678"))
        model.onEvent(PhoneVerificationInternalRoute.OnSendCodeClicked)
    }

    @Test
    fun `starts on the number phase with the injected country`() =
        runTest(testDispatcher) {
            val state = buildFixture().model.state.value

            assertEquals(PhoneVerificationPhase.Number, state.phase)
            assertEquals(brazil, state.country)
        }

    @Test
    fun `a number longer than the country allows is ignored, not shortened`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("11912345678"))
            assertEquals("11912345678", fixture.model.state.value.nationalNumber)

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("119123456789"))

            assertEquals("11912345678", fixture.model.state.value.nationalNumber)
        }

    @Test
    fun `a pasted foreign number is not cut into a brazilian one`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("+351912345678"))

            assertEquals("", fixture.model.state.value.nationalNumber)
        }

    @Test
    fun `switching to a country with a smaller cap empties an over-long number`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCountrySelected("US"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("12345678901234"))
            assertEquals("12345678901234", fixture.model.state.value.nationalNumber)

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCountrySelected("BR"))

            assertEquals("", fixture.model.state.value.nationalNumber)
        }

    @Test
    fun `selecting a country swaps the dial code`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCountrySelected("PT"))

            assertEquals("351", fixture.model.state.value.country.dialCode)
        }

    @Test
    fun `a brazilian leading zero is dropped before the digit cap`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("0 11 91234-5678"))

            assertEquals("11912345678", fixture.model.state.value.nationalNumber)
        }

    @Test
    fun `a pasted international number keeps only the national part`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("+55 11 91234-5678"))

            assertEquals("11912345678", fixture.model.state.value.nationalNumber)
        }

    @Test
    fun `an unknown country code is ignored`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCountrySelected("ZZ"))

            assertEquals(brazil, fixture.model.state.value.country)
        }

    @Test
    fun `an invalid number is rejected before calling firebase`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("1191234"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnSendCodeClicked)
            advanceUntilIdle()

            assertEquals(strings.errorInvalidPhoneNumber, fixture.model.state.value.message)
            assertTrue(fixture.phone.sendCodeCalls.isEmpty())
        }

    @Test
    fun `a valid number sends the e164 and moves to the code phase with the resend wait`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.typeValidBrazilianNumberAndSend()
            runCurrent()

            val state = fixture.model.state.value
            assertEquals(listOf<Pair<String, PhoneVerificationSession?>>("+5511912345678" to null), fixture.phone.sendCodeCalls)
            assertEquals(PhoneVerificationPhase.Code, state.phase)
            assertEquals("+5511912345678", state.sentToE164)
            assertEquals(RESEND_COOLDOWN_SECONDS, state.resendAvailableInSeconds)
        }

    @Test
    fun `another country builds its own e164`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCountrySelected("PT"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged("912 345 678"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnSendCodeClicked)
            advanceUntilIdle()

            assertEquals(
                "+351912345678",
                fixture.phone.sendCodeCalls
                    .single()
                    .first,
            )
        }

    @Test
    fun `auto verification stays on the number phase and refreshes the session`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = emailVerified).apply {
                    refreshSessionResult = Result.success(emailVerified.copy(phoneNumber = "+5511912345678"))
                }
            val phone = FakePhoneAuthSource(sendCodeResult = Result.success(PhoneCodeResult.AutoVerified(emailVerified)))
            val fixture = buildFixture(firebase = firebase, phone = phone)

            fixture.typeValidBrazilianNumberAndSend()
            advanceUntilIdle()

            assertEquals(PhoneVerificationPhase.Number, fixture.model.state.value.phase)
            assertEquals(1, firebase.refreshSessionCallCount)
            assertNull(fixture.model.state.value.message)
        }

    @Test
    fun `firebase rejecting the number shows the mapped message`() =
        runTest(testDispatcher) {
            val phone = FakePhoneAuthSource(sendCodeResult = Result.failure(VerificationError.InvalidPhoneNumber))
            val fixture = buildFixture(phone = phone)

            fixture.typeValidBrazilianNumberAndSend()
            advanceUntilIdle()

            assertEquals(PhoneVerificationPhase.Number, fixture.model.state.value.phase)
            assertEquals(strings.errorInvalidPhoneNumber, fixture.model.state.value.message)
        }

    @Test
    fun `code keeps only six digits`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("12a345678"))

            assertEquals("123456", fixture.model.state.value.code)
        }

    @Test
    fun `an incomplete code is rejected before calling firebase`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            fixture.typeValidBrazilianNumberAndSend()
            advanceUntilIdle()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("123"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked)
            advanceUntilIdle()

            assertEquals(strings.errorIncompleteCode, fixture.model.state.value.message)
            assertTrue(fixture.phone.confirmCodeCalls.isEmpty())
        }

    @Test
    fun `confirming the code links the phone and refreshes the session`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = emailVerified).apply {
                    refreshSessionResult = Result.success(emailVerified.copy(phoneNumber = "+5511912345678"))
                }
            val phone =
                FakePhoneAuthSource(
                    sendCodeResult = Result.success(PhoneCodeResult.CodeSent(sentSession)),
                    confirmCodeResult = Result.success(emailVerified),
                )
            val fixture = buildFixture(firebase = firebase, phone = phone)
            fixture.typeValidBrazilianNumberAndSend()
            advanceUntilIdle()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("123456"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked)
            advanceUntilIdle()

            assertSame(sentSession, phone.confirmCodeCalls.single().first)
            assertEquals("123456", phone.confirmCodeCalls.single().second)
            assertEquals(1, firebase.refreshSessionCallCount)
            assertNull(fixture.model.state.value.message)
        }

    @Test
    fun `a wrong code shows the mapped message`() =
        runTest(testDispatcher) {
            val phone =
                FakePhoneAuthSource(
                    sendCodeResult = Result.success(PhoneCodeResult.CodeSent(sentSession)),
                    confirmCodeResult = Result.failure(VerificationError.InvalidCode),
                )
            val fixture = buildFixture(phone = phone)
            fixture.typeValidBrazilianNumberAndSend()
            advanceUntilIdle()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("000000"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked)
            advanceUntilIdle()

            assertEquals(strings.errorInvalidCode, fixture.model.state.value.message)
            assertEquals(PhoneVerificationPhase.Code, fixture.model.state.value.phase)
        }

    @Test
    fun `a number already used by another account says so`() =
        runTest(testDispatcher) {
            val phone =
                FakePhoneAuthSource(
                    sendCodeResult = Result.success(PhoneCodeResult.CodeSent(sentSession)),
                    confirmCodeResult = Result.failure(VerificationError.PhoneAlreadyInUse),
                )
            val fixture = buildFixture(phone = phone)
            fixture.typeValidBrazilianNumberAndSend()
            advanceUntilIdle()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("123456"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked)
            advanceUntilIdle()

            assertEquals(strings.errorPhoneAlreadyInUse, fixture.model.state.value.message)
        }

    @Test
    fun `resend waits for the countdown and then reuses the previous session`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            fixture.typeValidBrazilianNumberAndSend()
            runCurrent()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnResendClicked)
            runCurrent()
            assertEquals(1, fixture.phone.sendCodeCalls.size)

            advanceTimeBy(RESEND_COOLDOWN_SECONDS * 1_000L)
            runCurrent()
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnResendClicked)
            runCurrent()

            assertEquals(2, fixture.phone.sendCodeCalls.size)
            assertEquals("+5511912345678", fixture.phone.sendCodeCalls[1].first)
            assertSame(sentSession, fixture.phone.sendCodeCalls[1].second)
        }

    @Test
    fun `changing the number goes back and clears the code`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            fixture.typeValidBrazilianNumberAndSend()
            runCurrent()
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("123"))

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnChangeNumberClicked)
            runCurrent()

            val state = fixture.model.state.value
            assertEquals(PhoneVerificationPhase.Number, state.phase)
            assertEquals("", state.code)
            assertNull(state.sentToE164)
            assertEquals(0, state.resendAvailableInSeconds)
            assertEquals("11912345678", state.nationalNumber)

            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(0, fixture.model.state.value.resendAvailableInSeconds)

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnResendClicked)
            runCurrent()
            assertEquals(1, fixture.phone.sendCodeCalls.size)
        }

    @Test
    fun `changing the number while a request is running cancels it`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            fixture.typeValidBrazilianNumberAndSend()
            runCurrent()
            advanceTimeBy(RESEND_COOLDOWN_SECONDS * 1_000L)
            runCurrent()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnResendClicked)
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnChangeNumberClicked)
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(PhoneVerificationPhase.Number, state.phase)
            assertNull(state.sentToE164)
            assertFalse(state.isLoading)
        }

    @Test
    fun `sending is ignored on the code phase`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            fixture.typeValidBrazilianNumberAndSend()
            runCurrent()
            assertEquals(PhoneVerificationPhase.Code, fixture.model.state.value.phase)

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnSendCodeClicked)
            runCurrent()

            assertEquals(1, fixture.phone.sendCodeCalls.size)
        }

    @Test
    fun `logging out calls the logout service`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnLogoutClicked)
            advanceUntilIdle()

            assertEquals(1, fixture.logoutService.logoutCallCount)
        }

    @Test
    fun `a failed logout shows a message instead of failing silently`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(logoutService = FakeLogoutService(result = Result.failure(IllegalStateException("offline"))))

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnLogoutClicked)
            advanceUntilIdle()

            assertEquals(strings.errorGeneric, fixture.model.state.value.message)
        }

    private val currentPhone = emailVerified.copy(phoneNumber = "+5511912345678")

    private fun Fixture.typeAndSend(national: String) {
        model.onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged(national))
        model.onEvent(PhoneVerificationInternalRoute.OnSendCodeClicked)
    }

    @Test
    fun `changing starts on the country of the current phone`() =
        runTest(testDispatcher) {
            val fixture =
                buildFixture(
                    firebase = FakeFirebaseAuthSource(initialUser = emailVerified.copy(phoneNumber = "+351912345678")),
                    mode = PhoneVerificationMode.Change,
                )
            advanceUntilIdle()

            assertEquals("PT", fixture.model.state.value.country.isoCode)
        }

    @Test
    fun `changing to the number already on the account is rejected without an sms`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(firebase = FakeFirebaseAuthSource(initialUser = currentPhone), mode = PhoneVerificationMode.Change)
            advanceUntilIdle()

            fixture.typeAndSend("(11) 91234-5678")
            advanceUntilIdle()

            assertEquals(strings.errorSamePhoneNumber, fixture.model.state.value.message)
            assertTrue(fixture.phone.sendCodeCalls.isEmpty())
        }

    @Test
    fun `changing asks firebase to replace the number, verifying asks to link it`() =
        runTest(testDispatcher) {
            val change = buildFixture(firebase = FakeFirebaseAuthSource(initialUser = currentPhone), mode = PhoneVerificationMode.Change)
            val verify = buildFixture()
            advanceUntilIdle()

            change.typeAndSend("(11) 98765-4321")
            verify.typeAndSend("(11) 98765-4321")
            advanceUntilIdle()

            assertEquals(listOf(PhoneCredentialPurpose.Replace), change.phone.sendCodePurposes)
            assertEquals(listOf(PhoneCredentialPurpose.Link), verify.phone.sendCodePurposes)
        }

    @Test
    fun `confirming a change marks the phone as changed`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = currentPhone).apply {
                    refreshSessionResult = Result.success(currentPhone.copy(phoneNumber = "+5511987654321"))
                }
            val phone =
                FakePhoneAuthSource(
                    sendCodeResult = Result.success(PhoneCodeResult.CodeSent(sentSession)),
                    confirmCodeResult = Result.success(currentPhone),
                )
            val fixture = buildFixture(firebase = firebase, phone = phone, mode = PhoneVerificationMode.Change)
            advanceUntilIdle()
            fixture.typeAndSend("(11) 98765-4321")
            advanceUntilIdle()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("123456"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked)
            advanceUntilIdle()

            assertTrue(fixture.model.state.value.isPhoneChanged)
        }

    @Test
    fun `an automatically verified change marks the phone as changed`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = currentPhone).apply {
                    refreshSessionResult = Result.success(currentPhone.copy(phoneNumber = "+5511987654321"))
                }
            val phone = FakePhoneAuthSource(sendCodeResult = Result.success(PhoneCodeResult.AutoVerified(currentPhone)))
            val fixture = buildFixture(firebase = firebase, phone = phone, mode = PhoneVerificationMode.Change)
            advanceUntilIdle()

            fixture.typeAndSend("(11) 98765-4321")
            advanceUntilIdle()

            assertTrue(fixture.model.state.value.isPhoneChanged)
        }

    @Test
    fun `verifying never marks the phone as changed`() =
        runTest(testDispatcher) {
            val firebase =
                FakeFirebaseAuthSource(initialUser = emailVerified).apply {
                    refreshSessionResult = Result.success(currentPhone)
                }
            val phone =
                FakePhoneAuthSource(
                    sendCodeResult = Result.success(PhoneCodeResult.CodeSent(sentSession)),
                    confirmCodeResult = Result.success(emailVerified),
                )
            val fixture = buildFixture(firebase = firebase, phone = phone)
            fixture.typeValidBrazilianNumberAndSend()
            advanceUntilIdle()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("123456"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked)
            advanceUntilIdle()

            assertFalse(fixture.model.state.value.isPhoneChanged)
        }

    @Test
    fun `a change that needs a recent login says so`() =
        runTest(testDispatcher) {
            val phone =
                FakePhoneAuthSource(
                    sendCodeResult = Result.success(PhoneCodeResult.CodeSent(sentSession)),
                    confirmCodeResult = Result.failure(VerificationError.RequiresRecentLogin),
                )
            val fixture = buildFixture(firebase = FakeFirebaseAuthSource(initialUser = currentPhone), phone = phone, mode = PhoneVerificationMode.Change)
            advanceUntilIdle()
            fixture.typeAndSend("(11) 98765-4321")
            advanceUntilIdle()

            fixture.model.onEvent(PhoneVerificationInternalRoute.OnCodeChanged("123456"))
            fixture.model.onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked)
            advanceUntilIdle()

            assertEquals(strings.errorRequiresRecentLogin, fixture.model.state.value.message)
            assertFalse(fixture.model.state.value.isPhoneChanged)
        }
}
