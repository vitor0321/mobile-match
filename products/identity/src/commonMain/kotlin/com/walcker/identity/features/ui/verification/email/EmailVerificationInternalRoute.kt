package com.walcker.identity.features.ui.verification.email

internal sealed interface EmailVerificationInternalRoute {
    data object OnConfirmedClicked : EmailVerificationInternalRoute

    data object OnResendClicked : EmailVerificationInternalRoute

    data object OnLogoutClicked : EmailVerificationInternalRoute

    data object OnMessageDismissed : EmailVerificationInternalRoute
}
