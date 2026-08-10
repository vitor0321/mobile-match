package com.walcker.identity.features.ui.forgotpassword

internal sealed interface ForgotPasswordInternalRoute {
    data class OnEmailChanged(val value: String) : ForgotPasswordInternalRoute
    data object OnSubmitClicked : ForgotPasswordInternalRoute
    data object OnBackClicked : ForgotPasswordInternalRoute
    data object OnErrorDismissed : ForgotPasswordInternalRoute
}
