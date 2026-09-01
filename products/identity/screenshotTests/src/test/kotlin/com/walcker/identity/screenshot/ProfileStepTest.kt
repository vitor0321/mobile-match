@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.ui.profile.ProfileScreen
import com.walcker.identity.features.ui.profile.ProfileState
import org.junit.Rule
import org.junit.Test

class ProfileStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun content_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ProfileScreen(
                    state = ProfileState(userSession = fakeUserSession),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun content_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ProfileScreen(
                    state = ProfileState(userSession = fakeUserSession),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun proContent_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ProfileScreen(
                    state =
                        ProfileState(
                            userSession = fakeUserSession,
                            isPro = true,
                            managementUrl = "https://play.google.com/store/account/subscriptions",
                        ),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun proContent_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ProfileScreen(
                    state =
                        ProfileState(
                            userSession = fakeUserSession,
                            isPro = true,
                            managementUrl = "https://play.google.com/store/account/subscriptions",
                        ),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun proContentManagementUnavailable_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ProfileScreen(
                    state =
                        ProfileState(
                            userSession = fakeUserSession,
                            isPro = true,
                            managementUrl = null,
                        ),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun loading_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ProfileScreen(
                    state =
                        ProfileState(
                            userSession = fakeUserSession,
                            isLoading = true,
                        ),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun loading_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ProfileScreen(
                    state =
                        ProfileState(
                            userSession = fakeUserSession,
                            isLoading = true,
                        ),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun error_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                ProfileScreen(
                    state =
                        ProfileState(
                            userSession = fakeUserSession,
                            error = "Não foi possível sair",
                        ),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun error_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                ProfileScreen(
                    state =
                        ProfileState(
                            userSession = fakeUserSession,
                            error = "Não foi possível sair",
                        ),
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }
}
