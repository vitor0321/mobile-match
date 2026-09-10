package com.walcker.identity.features.ui.login

internal sealed interface LoginInternalRoute {
    data class OnEmailChanged(
        val value: String,
    ) : LoginInternalRoute

    data class OnPasswordChanged(
        val value: String,
    ) : LoginInternalRoute

    data object OnSubmitClicked : LoginInternalRoute

    data object OnGoogleSignInClicked : LoginInternalRoute

    data object OnAppleSignInClicked : LoginInternalRoute

    data object OnSignUpClicked : LoginInternalRoute

    data object OnForgotPasswordClicked : LoginInternalRoute

    data object OnBackClicked : LoginInternalRoute

    data object OnErrorDismissed : LoginInternalRoute
}
