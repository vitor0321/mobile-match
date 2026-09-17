package com.walcker.identity.data.repository

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakePhoneAuthSource
import com.walcker.identity.features.data.repository.VerificationRepositoryImpl
import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class VerificationRepositoryImplTest {
    private val unverifiedPhone =
        UserSession(uid = "uid-1", email = "ana@match.app", displayName = "Ana", isEmailVerified = true)
    private val verifiedPhone = unverifiedPhone.copy(phoneNumber = "+5511912345678")
    private val session =
        PhoneVerificationSession(e164 = "+5511912345678", verificationId = "verification-1", platformResendToken = null)

    private fun repository(
        firebase: FakeFirebaseAuthSource,
        phone: FakePhoneAuthSource,
    ) = VerificationRepositoryImpl(firebaseAuthSource = firebase, phoneAuthSource = phone)

    @Test
    fun `code sent passes through without refreshing the session`() =
        runTest {
            val firebase = FakeFirebaseAuthSource(initialUser = unverifiedPhone)
            val phone = FakePhoneAuthSource(sendCodeResult = Result.success(PhoneCodeResult.CodeSent(session)))

            val result = repository(firebase, phone).sendPhoneCode("+5511912345678").getOrThrow()

            assertIs<PhoneCodeResult.CodeSent>(result)
            assertEquals(0, firebase.refreshSessionCallCount)
            assertEquals<List<Pair<String, PhoneVerificationSession?>>>(listOf("+5511912345678" to null), phone.sendCodeCalls)
        }

    @Test
    fun `resend forwards the previous session to the source`() =
        runTest {
            val firebase = FakeFirebaseAuthSource(initialUser = unverifiedPhone)
            val phone = FakePhoneAuthSource()

            repository(firebase, phone).sendPhoneCode("+5511912345678", resend = session)

            assertSame(session, phone.sendCodeCalls.single().second)
        }

    @Test
    fun `auto verification refreshes the session and returns the refreshed user`() =
        runTest {
            val firebase =
                FakeFirebaseAuthSource(initialUser = unverifiedPhone).apply {
                    refreshSessionResult = Result.success(verifiedPhone)
                }
            val phone = FakePhoneAuthSource(sendCodeResult = Result.success(PhoneCodeResult.AutoVerified(unverifiedPhone)))

            val result = repository(firebase, phone).sendPhoneCode("+5511912345678").getOrThrow()

            assertEquals(PhoneCodeResult.AutoVerified(verifiedPhone), result)
            assertEquals(1, firebase.refreshSessionCallCount)
        }

    @Test
    fun `already linked while sending is treated as auto verified`() =
        runTest {
            val firebase =
                FakeFirebaseAuthSource(initialUser = unverifiedPhone).apply {
                    refreshSessionResult = Result.success(verifiedPhone)
                }
            val phone = FakePhoneAuthSource(sendCodeResult = Result.failure(VerificationError.AlreadyLinked))

            val result = repository(firebase, phone).sendPhoneCode("+5511912345678").getOrThrow()

            assertEquals(PhoneCodeResult.AutoVerified(verifiedPhone), result)
        }

    @Test
    fun `other send failures propagate untouched`() =
        runTest {
            val firebase = FakeFirebaseAuthSource(initialUser = unverifiedPhone)
            val phone = FakePhoneAuthSource(sendCodeResult = Result.failure(VerificationError.InvalidPhoneNumber))

            val error = repository(firebase, phone).sendPhoneCode("+55119").exceptionOrNull()

            assertEquals(VerificationError.InvalidPhoneNumber, error)
            assertEquals(0, firebase.refreshSessionCallCount)
        }

    @Test
    fun `confirming a code refreshes the session afterwards`() =
        runTest {
            val firebase =
                FakeFirebaseAuthSource(initialUser = unverifiedPhone).apply {
                    refreshSessionResult = Result.success(verifiedPhone)
                }
            val phone = FakePhoneAuthSource(confirmCodeResult = Result.success(unverifiedPhone))

            val result = repository(firebase, phone).confirmPhoneCode(session, "123456").getOrThrow()

            assertEquals(verifiedPhone, result)
            assertEquals(1, firebase.refreshSessionCallCount)
            assertEquals("123456", phone.confirmCodeCalls.single().second)
        }

    @Test
    fun `already linked while confirming is a success`() =
        runTest {
            val firebase =
                FakeFirebaseAuthSource(initialUser = unverifiedPhone).apply {
                    refreshSessionResult = Result.success(verifiedPhone)
                }
            val phone = FakePhoneAuthSource(confirmCodeResult = Result.failure(VerificationError.AlreadyLinked))

            val result = repository(firebase, phone).confirmPhoneCode(session, "123456")

            assertEquals(verifiedPhone, result.getOrThrow())
        }

    @Test
    fun `replacing forwards the replace purpose and refreshes after confirming`() =
        runTest {
            val replaceSession =
                PhoneVerificationSession(
                    e164 = "+5511987654321",
                    verificationId = "verification-2",
                    platformResendToken = null,
                    purpose = PhoneCredentialPurpose.Replace,
                )
            val firebase =
                FakeFirebaseAuthSource(initialUser = verifiedPhone).apply {
                    refreshSessionResult = Result.success(verifiedPhone.copy(phoneNumber = "+5511987654321"))
                }
            val phone =
                FakePhoneAuthSource(
                    sendCodeResult = Result.success(PhoneCodeResult.CodeSent(replaceSession)),
                    confirmCodeResult = Result.success(verifiedPhone),
                )
            val repository = repository(firebase, phone)

            repository.sendPhoneCode("+5511987654321", purpose = PhoneCredentialPurpose.Replace)
            val result = repository.confirmPhoneCode(replaceSession, "123456").getOrThrow()

            assertEquals(listOf(PhoneCredentialPurpose.Replace), phone.sendCodePurposes)
            assertEquals("+5511987654321", result.phoneNumber)
            assertEquals(1, firebase.refreshSessionCallCount)
        }

    @Test
    fun `already linked is not a success when replacing`() =
        runTest {
            val replaceSession =
                PhoneVerificationSession(
                    e164 = "+5511987654321",
                    verificationId = "verification-2",
                    platformResendToken = null,
                    purpose = PhoneCredentialPurpose.Replace,
                )
            val firebase = FakeFirebaseAuthSource(initialUser = verifiedPhone)
            val phone = FakePhoneAuthSource(confirmCodeResult = Result.failure(VerificationError.AlreadyLinked))

            val error = repository(firebase, phone).confirmPhoneCode(replaceSession, "123456").exceptionOrNull()

            assertEquals(VerificationError.AlreadyLinked, error)
            assertEquals(0, firebase.refreshSessionCallCount)
        }

    @Test
    fun `wrong code fails without refreshing`() =
        runTest {
            val firebase = FakeFirebaseAuthSource(initialUser = unverifiedPhone)
            val phone = FakePhoneAuthSource(confirmCodeResult = Result.failure(VerificationError.InvalidCode))

            val error = repository(firebase, phone).confirmPhoneCode(session, "000000").exceptionOrNull()

            assertEquals(VerificationError.InvalidCode, error)
            assertEquals(0, firebase.refreshSessionCallCount)
        }
}
