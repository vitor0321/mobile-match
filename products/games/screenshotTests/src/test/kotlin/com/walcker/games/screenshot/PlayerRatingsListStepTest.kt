@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.shared.playerRatings.PlayerRatingsContent
import com.walcker.games.features.ui.shared.playerRatings.PlayerRatingsState
import com.walcker.games.strings.PtBrGamesStrings
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test

class PlayerRatingsListStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        PlayerRatingsState(
            userId = "player-1",
            playerName = "Bruno Lima",
            ratings = persistentListOf(fakeRating(id = "1"), fakeRating(id = "2", stars = 3, comment = "Bom, mas chegou atrasado.")),
            hasMore = true,
        )

    private fun snapshot(
        state: PlayerRatingsState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                PlayerRatingsContent(state = state, onEvent = {}, strings = PtBrGamesStrings.playerRatings)
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(loadedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun empty_lightMode() = snapshot(PlayerRatingsState(userId = "player-1", playerName = "Bruno Lima"), darkTheme = false)

    @Test
    fun loadingFirstPage_lightMode() = snapshot(PlayerRatingsState(userId = "player-1", playerName = "Bruno Lima", isLoadingFirstPage = true), darkTheme = false)
}
