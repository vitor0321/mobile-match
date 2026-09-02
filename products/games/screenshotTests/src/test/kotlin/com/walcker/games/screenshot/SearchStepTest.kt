@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.search.SearchContent
import com.walcker.games.features.ui.search.SearchState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test

class SearchStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val resultsState =
        SearchState(
            query = "centro",
            results = persistentListOf(fakeGame(id = "match-1"), fakeGame(id = "match-2", venueName = "Arena Vila Nova")),
        )

    private fun snapshot(
        state: SearchState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                SearchContent(state = state, onEvent = {})
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(resultsState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(resultsState, darkTheme = true)

    @Test
    fun loading_lightMode() = snapshot(SearchState(isLoading = true), darkTheme = false)

    @Test
    fun emptyForQuery_lightMode() = snapshot(SearchState(query = "quadra inexistente"), darkTheme = false)

    @Test
    fun filtersPanel_lightMode() = snapshot(resultsState.copy(showFiltersPanel = true), darkTheme = false)
}
