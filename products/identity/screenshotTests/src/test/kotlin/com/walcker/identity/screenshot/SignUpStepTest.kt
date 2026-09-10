@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.ui.signup.SignUpScreen
import com.walcker.identity.features.ui.signup.SignUpState
import org.junit.Rule
import org.junit.Test

class SignUpStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun content_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                SignUpScreen(
                    state =
                        SignUpState(
                            fullName = "Jonathan Tomaz",
                            email = "user@match.app",
                            password = "123456",
                            confirmPassword = "123456",
                        ),
                    onFullNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSubmit = {},
                    onLogin = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun content_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                SignUpScreen(
                    state =
                        SignUpState(
                            fullName = "Jonathan Tomaz",
                            email = "user@match.app",
                            password = "123456",
                            confirmPassword = "123456",
                        ),
                    onFullNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSubmit = {},
                    onLogin = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun error_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                SignUpScreen(
                    state =
                        SignUpState(
                            email = "user@match.app",
                            error = "Não foi possível criar a conta",
                        ),
                    onFullNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSubmit = {},
                    onLogin = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun error_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                SignUpScreen(
                    state =
                        SignUpState(
                            email = "user@match.app",
                            error = "Não foi possível criar a conta",
                        ),
                    onFullNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSubmit = {},
                    onLogin = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun loading_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                SignUpScreen(
                    state =
                        SignUpState(
                            fullName = "Jonathan Tomaz",
                            email = "user@match.app",
                            password = "123456",
                            confirmPassword = "123456",
                            isLoading = true,
                        ),
                    onFullNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSubmit = {},
                    onLogin = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun loading_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                SignUpScreen(
                    state =
                        SignUpState(
                            fullName = "Jonathan Tomaz",
                            email = "user@match.app",
                            password = "123456",
                            confirmPassword = "123456",
                            isLoading = true,
                        ),
                    onFullNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSubmit = {},
                    onLogin = {},
                    onBack = {},
                )
            }
        }
    }
}
