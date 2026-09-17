package com.walcker.identity.features.ui.verification.phone

internal sealed interface PhoneVerificationInternalRoute {
    data class OnCountrySelected(
        val isoCode: String,
    ) : PhoneVerificationInternalRoute

    data class OnNationalNumberChanged(
        val value: String,
    ) : PhoneVerificationInternalRoute

    data object OnSendCodeClicked : PhoneVerificationInternalRoute

    data class OnCodeChanged(
        val value: String,
    ) : PhoneVerificationInternalRoute

    data object OnConfirmCodeClicked : PhoneVerificationInternalRoute

    data object OnResendClicked : PhoneVerificationInternalRoute

    data object OnChangeNumberClicked : PhoneVerificationInternalRoute

    data object OnLogoutClicked : PhoneVerificationInternalRoute

    data object OnMessageDismissed : PhoneVerificationInternalRoute
}
