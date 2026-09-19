@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.home.GameListContent
import com.walcker.games.features.ui.home.GameListState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test

class GameListStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        GameListState(
            isLoading = false,
            games = persistentListOf(fakeGame(id = "match-1"), fakeGame(id = "match-2", venueName = "Arena Vila Nova")),
            preferencesLoaded = true,
        )

    private fun snapshot(
        state: GameListState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                GameListContent(state = state, onEvent = {})
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(loadedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun loading_lightMode() = snapshot(GameListState(isLoading = true, preferencesLoaded = true), darkTheme = false)

    @Test
    fun empty_lightMode() = snapshot(GameListState(isLoading = false, preferencesLoaded = true), darkTheme = false)

    @Test
    fun mapMode_lightMode() {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = false) {
                GameListContent(state = loadedState, onEvent = {}, showMap = true)
            }
        }
    }

    @Test
    fun error_lightMode() =
        snapshot(
            GameListState(isLoading = false, preferencesLoaded = true, errorMessage = "Não foi possível carregar as partidas."),
            darkTheme = false,
        )
}
