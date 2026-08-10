package com.walcker.identity.features.ui.forgotpassword

import androidx.compose.runtime.Immutable

@Immutable
internal data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)
