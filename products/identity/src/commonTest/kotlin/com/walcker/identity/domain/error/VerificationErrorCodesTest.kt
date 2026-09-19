package com.walcker.identity.domain.error

import com.walcker.identity.features.domain.error.VerificationError
import com.walcker.identity.features.domain.error.verificationErrorForAndroidCode
import com.walcker.identity.features.domain.error.verificationErrorForIosCode
import kotlin.test.Test
import kotlin.test.assertEquals

class VerificationErrorCodesTest {
    @Test
    fun `android codes map to typed verification errors`() {
        assertEquals(VerificationError.TooManyRequests, verificationErrorForAndroidCode("ERROR_TOO_MANY_REQUESTS"))
        assertEquals(VerificationError.TooManyRequests, verificationErrorForAndroidCode("ERROR_QUOTA_EXCEEDED"))
        assertEquals(VerificationError.InvalidPhoneNumber, verificationErrorForAndroidCode("ERROR_INVALID_PHONE_NUMBER"))
        assertEquals(VerificationError.InvalidPhoneNumber, verificationErrorForAndroidCode("ERROR_MISSING_PHONE_NUMBER"))
        assertEquals(VerificationError.InvalidCode, verificationErrorForAndroidCode("ERROR_INVALID_VERIFICATION_CODE"))
        assertEquals(VerificationError.InvalidCode, verificationErrorForAndroidCode("ERROR_MISSING_VERIFICATION_CODE"))
        assertEquals(VerificationError.CodeExpired, verificationErrorForAndroidCode("ERROR_SESSION_EXPIRED"))
        assertEquals(VerificationError.CodeExpired, verificationErrorForAndroidCode("ERROR_INVALID_VERIFICATION_ID"))
        assertEquals(VerificationError.PhoneAlreadyInUse, verificationErrorForAndroidCode("ERROR_CREDENTIAL_ALREADY_IN_USE"))
        assertEquals(VerificationError.AlreadyLinked, verificationErrorForAndroidCode("ERROR_PROVIDER_ALREADY_LINKED"))
    }

    @Test
    fun `unknown or missing android code is unknown`() {
        assertEquals(VerificationError.Unknown, verificationErrorForAndroidCode("ERROR_SOMETHING_NEW"))
        assertEquals(VerificationError.Unknown, verificationErrorForAndroidCode(null))
    }

    @Test
    fun `ios codes map to typed verification errors`() {
        assertEquals(VerificationError.TooManyRequests, verificationErrorForIosCode(17010L))
        assertEquals(VerificationError.TooManyRequests, verificationErrorForIosCode(17052L))
        assertEquals(VerificationError.Network, verificationErrorForIosCode(17020L))
        assertEquals(VerificationError.InvalidPhoneNumber, verificationErrorForIosCode(17041L))
        assertEquals(VerificationError.InvalidPhoneNumber, verificationErrorForIosCode(17042L))
        assertEquals(VerificationError.InvalidCode, verificationErrorForIosCode(17043L))
        assertEquals(VerificationError.InvalidCode, verificationErrorForIosCode(17044L))
        assertEquals(VerificationError.CodeExpired, verificationErrorForIosCode(17046L))
        assertEquals(VerificationError.CodeExpired, verificationErrorForIosCode(17051L))
        assertEquals(VerificationError.PhoneAlreadyInUse, verificationErrorForIosCode(17025L))
        assertEquals(VerificationError.AlreadyLinked, verificationErrorForIosCode(17015L))
    }

    @Test
    fun `requiring a recent login maps on both platforms`() {
        assertEquals(VerificationError.RequiresRecentLogin, verificationErrorForAndroidCode("ERROR_REQUIRES_RECENT_LOGIN"))
        assertEquals(VerificationError.RequiresRecentLogin, verificationErrorForIosCode(17014L))
    }

    @Test
    fun `unknown ios code is unknown`() {
        assertEquals(VerificationError.Unknown, verificationErrorForIosCode(99999L))
    }
}
