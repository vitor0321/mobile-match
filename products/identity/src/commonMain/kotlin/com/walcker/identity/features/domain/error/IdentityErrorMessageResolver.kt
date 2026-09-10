package com.walcker.identity.features.domain.error

import com.walcker.identity.strings.ForgotPasswordStrings
import com.walcker.identity.strings.LoginStrings
import com.walcker.identity.strings.SignUpStrings

internal fun IdentityError.loginMessage(strings: LoginStrings): String? =
    when (this) {
        IdentityError.Cancelled -> null
        else -> strings.signInError
    }

internal fun IdentityError.googleSignInMessage(strings: LoginStrings): String? =
    when (this) {
        IdentityError.Cancelled -> null
        else -> strings.googleSignInError
    }

internal fun IdentityError.appleSignInMessage(strings: LoginStrings): String? =
    when (this) {
        IdentityError.Cancelled -> null
        else -> strings.appleSignInError
    }

internal fun IdentityError.signUpMessage(strings: SignUpStrings): String? =
    when (this) {
        IdentityError.Cancelled -> null
        else -> strings.signUpError
    }

internal fun IdentityError.passwordResetMessage(strings: ForgotPasswordStrings): String? =
    when (this) {
        IdentityError.Cancelled -> null
        else -> strings.sendEmailError
    }
