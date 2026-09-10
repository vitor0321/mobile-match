@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordScreen
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordState
import org.junit.Rule
import org.junit.Test

class ForgotPasswordStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun content_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun content_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun loading_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                            isLoading = true,
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun loading_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                            isLoading = true,
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun error_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                            error = "E-mail inválido",
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun error_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                            error = "E-mail inválido",
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun success_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                            isSuccess = true,
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun success_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ForgotPasswordScreen(
                    state =
                        ForgotPasswordState(
                            email = "user@match.app",
                            isSuccess = true,
                        ),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }
}
