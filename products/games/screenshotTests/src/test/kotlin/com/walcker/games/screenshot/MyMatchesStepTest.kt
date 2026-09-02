@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.ui.myMatches.MyMatchesContent
import com.walcker.games.features.ui.myMatches.MyMatchesState
import com.walcker.games.features.ui.myMatches.MyMatchesTab
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class MyMatchesStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val activeState =
        MyMatchesState(
            activeTab = MyMatchesTab.ACTIVE,
            active =
                listOf(
                    fakeMyMatch(game = fakeGame(id = "match-1"), role = MatchRole.ORGANIZER),
                    fakeMyMatch(game = fakeGame(id = "match-2", venueName = "Arena Vila Nova"), role = MatchRole.PARTICIPANT),
                ),
        )

    private fun snapshot(
        state: MyMatchesState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                MyMatchesContent(state = state, onEvent = {}, strings = PtBrGamesStrings.myMatches)
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(activeState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(activeState, darkTheme = true)

    @Test
    fun emptyActive_lightMode() = snapshot(MyMatchesState(activeTab = MyMatchesTab.ACTIVE), darkTheme = false)

    @Test
    fun emptyPast_lightMode() = snapshot(MyMatchesState(activeTab = MyMatchesTab.PAST), darkTheme = false)

    @Test
    fun loading_lightMode() = snapshot(MyMatchesState(isLoading = true), darkTheme = false)
}
