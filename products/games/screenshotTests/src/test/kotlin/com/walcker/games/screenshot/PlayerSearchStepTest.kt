@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.shared.playerSearch.PlayerSearchContent
import com.walcker.games.features.ui.shared.playerSearch.PlayerSearchState
import com.walcker.games.strings.PtBrGamesStrings
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test

class PlayerSearchStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val resultsState =
        PlayerSearchState(
            query = "bruno",
            results = persistentListOf(fakePlayerSearchResult(userId = "player-1"), fakePlayerSearchResult(userId = "player-2", displayName = "Carla Nunes")),
        )

    private fun snapshot(
        state: PlayerSearchState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                PlayerSearchContent(state = state, onEvent = {}, strings = PtBrGamesStrings.playerSearch)
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(resultsState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(resultsState, darkTheme = true)

    @Test
    fun idle_lightMode() = snapshot(PlayerSearchState(), darkTheme = false)

    @Test
    fun loading_lightMode() = snapshot(PlayerSearchState(isLoading = true), darkTheme = false)

    @Test
    fun filtersPanel_lightMode() = snapshot(resultsState.copy(showFiltersPanel = true), darkTheme = false)
}
