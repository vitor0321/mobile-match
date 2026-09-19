package com.walcker.identity.api

import com.walcker.identity.features.domain.phone.BRAZIL_DIAL_CODE
import com.walcker.identity.features.domain.phone.formatBrazilianNational

public data class UserSession(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val creationTimestamp: Long? = null,
    val isEmailVerified: Boolean = false,
    val phoneNumber: String? = null,
)

public enum class VerificationStage { Email, Phone, Done }

public fun UserSession.verificationStage(): VerificationStage =
    when {
        !isEmailVerified -> VerificationStage.Email
        phoneNumber.isNullOrBlank() -> VerificationStage.Phone
        else -> VerificationStage.Done
    }

public fun UserSession.needsVerification(): Boolean = verificationStage() != VerificationStage.Done

public fun UserSession.formattedPhoneNumber(): String? {
    val phone = phoneNumber?.takeIf { it.isNotBlank() } ?: return null
    val brazilPrefix = "+$BRAZIL_DIAL_CODE"
    return if (phone.startsWith(brazilPrefix)) {
        "$brazilPrefix ${formatBrazilianNational(phone.removePrefix(brazilPrefix))}"
    } else {
        phone
    }
}
