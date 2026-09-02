@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.playerProfile.PlayerProfileContent
import com.walcker.games.features.ui.playerProfile.PlayerProfileState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class PlayerProfileStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        PlayerProfileState(
            userName = "Ana Souza",
            userEmail = "ana@example.com",
            matchesOrganized = 5,
            matchesParticipated = 12,
            ratings = listOf(fakeRating(id = "1"), fakeRating(id = "2")),
            averageRating = 4.6f,
            totalRatings = 2,
            isAvailable = true,
        )

    private fun snapshot(
        state: PlayerProfileState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                PlayerProfileContent(state = state, onEvent = {}, strings = PtBrGamesStrings.playerProfile)
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(loadedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun visitor_lightMode() = snapshot(PlayerProfileState(), darkTheme = false)

    @Test
    fun loading_lightMode() = snapshot(PlayerProfileState(isLoading = true), darkTheme = false)
}
