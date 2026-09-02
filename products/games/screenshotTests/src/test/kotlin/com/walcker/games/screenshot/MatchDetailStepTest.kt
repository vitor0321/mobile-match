@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.shared.matchDetail.MatchDetailContent
import com.walcker.games.features.ui.shared.matchDetail.MatchDetailState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class MatchDetailStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        MatchDetailState(
            isLoading = false,
            match = fakeGame(id = "match-1", participants = listOf("player-1")),
            participants = fakeParticipantsSummary(),
            currentUserId = "player-1",
        )

    private fun snapshot(
        state: MatchDetailState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                MatchDetailContent(state = state, strings = PtBrGamesStrings, onEvent = {})
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(loadedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun loading_lightMode() = snapshot(MatchDetailState(), darkTheme = false)

    @Test
    fun error_lightMode() = snapshot(MatchDetailState(isLoading = false, errorMessage = "Não foi possível carregar a partida."), darkTheme = false)

    @Test
    fun promoted_lightMode() = snapshot(loadedState.copy(justPromoted = true), darkTheme = false)

    @Test
    fun organizerView_lightMode() =
        snapshot(
            loadedState.copy(
                match = loadedState.match?.copy(organizerId = "player-1", participants = listOf("player-1")),
            ),
            darkTheme = false,
        )
}
