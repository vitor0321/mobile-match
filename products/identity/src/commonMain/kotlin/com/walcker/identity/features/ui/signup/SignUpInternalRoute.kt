package com.walcker.identity.features.ui.signup

internal sealed interface SignUpInternalRoute {
    data class OnFullNameChanged(
        val value: String,
    ) : SignUpInternalRoute

    data class OnEmailChanged(
        val value: String,
    ) : SignUpInternalRoute

    data class OnPasswordChanged(
        val value: String,
    ) : SignUpInternalRoute

    data class OnConfirmPasswordChanged(
        val value: String,
    ) : SignUpInternalRoute

    data object OnSubmitClicked : SignUpInternalRoute

    data object OnGoogleSignInClicked : SignUpInternalRoute

    data object OnAppleSignInClicked : SignUpInternalRoute

    data object OnLoginClicked : SignUpInternalRoute

    data object OnBackClicked : SignUpInternalRoute

    data object OnErrorDismissed : SignUpInternalRoute
}
