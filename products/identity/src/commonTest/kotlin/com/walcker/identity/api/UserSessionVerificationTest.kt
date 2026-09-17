package com.walcker.identity.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserSessionVerificationTest {
    private fun session(
        isEmailVerified: Boolean,
        phoneNumber: String?,
    ) = UserSession(
        uid = "uid-1",
        email = "ana@match.app",
        displayName = "Ana",
        isEmailVerified = isEmailVerified,
        phoneNumber = phoneNumber,
    )

    @Test
    fun `unverified email is the first stage even when a phone exists`() {
        assertEquals(VerificationStage.Email, session(isEmailVerified = false, phoneNumber = null).verificationStage())
        assertEquals(VerificationStage.Email, session(isEmailVerified = false, phoneNumber = "+5511912345678").verificationStage())
    }

    @Test
    fun `verified email without a phone goes to the phone stage`() {
        assertEquals(VerificationStage.Phone, session(isEmailVerified = true, phoneNumber = null).verificationStage())
        assertEquals(VerificationStage.Phone, session(isEmailVerified = true, phoneNumber = "").verificationStage())
        assertEquals(VerificationStage.Phone, session(isEmailVerified = true, phoneNumber = "   ").verificationStage())
    }

    @Test
    fun `verified email and phone need nothing else`() {
        val verified = session(isEmailVerified = true, phoneNumber = "+5511912345678")

        assertEquals(VerificationStage.Done, verified.verificationStage())
        assertFalse(verified.needsVerification())
    }

    @Test
    fun `any missing stage needs verification`() {
        assertTrue(session(isEmailVerified = false, phoneNumber = null).needsVerification())
        assertTrue(session(isEmailVerified = true, phoneNumber = null).needsVerification())
    }

    @Test
    fun `a brazilian phone is shown with the national mask`() {
        assertEquals("+55 (11) 91234-5678", session(isEmailVerified = true, phoneNumber = "+5511912345678").formattedPhoneNumber())
    }

    @Test
    fun `other countries are shown as e164 and no phone shows nothing`() {
        assertEquals("+351912345678", session(isEmailVerified = true, phoneNumber = "+351912345678").formattedPhoneNumber())
        assertEquals(null, session(isEmailVerified = true, phoneNumber = null).formattedPhoneNumber())
    }

    @Test
    fun `new fields default to unverified so existing call sites keep compiling`() {
        val legacy = UserSession(uid = "uid-1", email = "ana@match.app", displayName = "Ana")

        assertFalse(legacy.isEmailVerified)
        assertEquals(null, legacy.phoneNumber)
    }
}
