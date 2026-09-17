package com.walcker.identity.features.ui.verification.email

internal data class EmailVerificationState(
    val email: String = "",
    val isSending: Boolean = false,
    val isChecking: Boolean = false,
    val resendAvailableInSeconds: Int = 0,
    val message: String? = null,
    val isMessageError: Boolean = false,
)
