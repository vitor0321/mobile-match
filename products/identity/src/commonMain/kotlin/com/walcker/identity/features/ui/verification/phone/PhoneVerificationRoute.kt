package com.walcker.identity.features.ui.verification.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.walcker.identity.features.data.platform.localizedCountryName

@Composable
internal fun PhoneVerificationRoute(
    stepModel: PhoneVerificationStepModel,
    mode: PhoneVerificationMode,
    onBack: () -> Unit = {},
) {
    val state by stepModel.state.collectAsState()
    var isCountryPickerVisible by remember { mutableStateOf(false) }
    val countryName = remember(state.country.isoCode) { localizedCountryName(state.country.isoCode) }

    PhoneVerificationEvents(stepModel = stepModel) { onEvent ->
        PhoneVerificationScreen(
            state = state,
            countryName = countryName,
            onCountryClick = { isCountryPickerVisible = true },
            onNationalNumberChanged = { onEvent(PhoneVerificationInternalRoute.OnNationalNumberChanged(it)) },
            onSendCode = { onEvent(PhoneVerificationInternalRoute.OnSendCodeClicked) },
            onCodeChanged = { onEvent(PhoneVerificationInternalRoute.OnCodeChanged(it)) },
            onConfirmCode = { onEvent(PhoneVerificationInternalRoute.OnConfirmCodeClicked) },
            onResend = { onEvent(PhoneVerificationInternalRoute.OnResendClicked) },
            onChangeNumber = { onEvent(PhoneVerificationInternalRoute.OnChangeNumberClicked) },
            onLogout = { onEvent(PhoneVerificationInternalRoute.OnLogoutClicked) },
            mode = mode,
            onBack = onBack,
        )

        if (isCountryPickerVisible) {
            CountryPickerSheet(
                selectedIsoCode = state.country.isoCode,
                onSelect = { isoCode ->
                    onEvent(PhoneVerificationInternalRoute.OnCountrySelected(isoCode))
                    isCountryPickerVisible = false
                },
                onDismiss = { isCountryPickerVisible = false },
            )
        }
    }
}
