package com.walcker.identity.features.ui.verification.phone

import com.walcker.identity.features.domain.phone.CountryDialCode

internal const val PHONE_CODE_LENGTH = 6

internal enum class PhoneVerificationPhase { Number, Code }

internal enum class PhoneVerificationMode { Verify, Change }

internal data class PhoneVerificationState(
    val country: CountryDialCode,
    val phase: PhoneVerificationPhase = PhoneVerificationPhase.Number,
    val nationalNumber: String = "",
    val code: String = "",
    val sentToE164: String? = null,
    val isLoading: Boolean = false,
    val resendAvailableInSeconds: Int = 0,
    val message: String? = null,
    val isPhoneChanged: Boolean = false,
)
