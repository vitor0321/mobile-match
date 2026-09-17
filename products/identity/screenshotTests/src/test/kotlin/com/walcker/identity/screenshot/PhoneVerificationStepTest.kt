@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.domain.phone.CountryDialCode
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationMode
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationPhase
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationScreen
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationState
import org.junit.Rule
import org.junit.Test

class PhoneVerificationStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val brazil = CountryDialCode(isoCode = "BR", dialCode = "55")

    private fun snapshot(
        state: PhoneVerificationState,
        darkTheme: Boolean = false,
        mode: PhoneVerificationMode = PhoneVerificationMode.Verify,
    ) {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = darkTheme) {
                PhoneVerificationScreen(
                    state = state,
                    countryName = "Brasil",
                    onCountryClick = {},
                    onNationalNumberChanged = {},
                    onSendCode = {},
                    onCodeChanged = {},
                    onConfirmCode = {},
                    onResend = {},
                    onChangeNumber = {},
                    onLogout = {},
                    mode = mode,
                )
            }
        }
    }

    @Test
    fun number_lightMode() = snapshot(PhoneVerificationState(country = brazil, nationalNumber = "11912345678"))

    @Test
    fun number_darkMode() = snapshot(PhoneVerificationState(country = brazil, nationalNumber = "11912345678"), darkTheme = true)

    @Test
    fun code_lightMode() =
        snapshot(
            PhoneVerificationState(
                country = brazil,
                phase = PhoneVerificationPhase.Code,
                nationalNumber = "11912345678",
                sentToE164 = "+5511912345678",
                code = "123",
                resendAvailableInSeconds = 37,
            ),
        )

    @Test
    fun error_lightMode() =
        snapshot(
            PhoneVerificationState(
                country = brazil,
                phase = PhoneVerificationPhase.Code,
                sentToE164 = "+5511912345678",
                code = "000000",
                message = "Código incorreto. Confira o SMS e tente de novo.",
            ),
        )

    @Test
    fun change_lightMode() =
        snapshot(
            PhoneVerificationState(country = brazil, nationalNumber = "11912345678"),
            mode = PhoneVerificationMode.Change,
        )
}
