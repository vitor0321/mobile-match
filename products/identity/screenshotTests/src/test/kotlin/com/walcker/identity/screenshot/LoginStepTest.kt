@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.ui.login.LoginScreen
import com.walcker.identity.features.ui.login.LoginState
import org.junit.Rule
import org.junit.Test

class LoginStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun content_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            password = "123456",
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                )
            }
        }
    }

    @Test
    fun content_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            password = "123456",
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                )
            }
        }
    }

    @Test
    fun error_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            error = "Não foi possível entrar",
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                )
            }
        }
    }

    @Test
    fun error_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            error = "Não foi possível entrar",
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                )
            }
        }
    }

    @Test
    fun loading_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            password = "123456",
                            isLoading = true,
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                )
            }
        }
    }

    @Test
    fun loading_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            password = "123456",
                            isLoading = true,
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                )
            }
        }
    }

    @Test
    fun appleAvailable_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            password = "123456",
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                    isAppleSignInAvailable = true,
                    onAppleSignIn = {},
                )
            }
        }
    }

    @Test
    fun appleAvailable_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                LoginScreen(
                    state =
                        LoginState(
                            email = "user@match.app",
                            password = "123456",
                        ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onGoogleSignIn = {},
                    onSignUp = {},
                    onBack = {},
                    onForgotPassword = {},
                    isAppleSignInAvailable = true,
                    onAppleSignIn = {},
                )
            }
        }
    }
}
