@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.ui.verification.email.EmailVerificationScreen
import com.walcker.identity.features.ui.verification.email.EmailVerificationState
import org.junit.Rule
import org.junit.Test

class EmailVerificationStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private fun snapshot(
        state: EmailVerificationState,
        darkTheme: Boolean = false,
    ) {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = darkTheme) {
                EmailVerificationScreen(state = state, onConfirmed = {}, onResend = {}, onLogout = {})
            }
        }
    }

    @Test
    fun sent_lightMode() = snapshot(EmailVerificationState(email = "user@match.app", message = "E-mail enviado.", resendAvailableInSeconds = 42))

    @Test
    fun sent_darkMode() = snapshot(EmailVerificationState(email = "user@match.app", message = "E-mail enviado.", resendAvailableInSeconds = 42), darkTheme = true)

    @Test
    fun notConfirmed_lightMode() =
        snapshot(
            EmailVerificationState(
                email = "user@match.app",
                message = "Ainda não recebemos a confirmação. Abra o link no seu e-mail.",
                isMessageError = true,
            ),
        )
}
