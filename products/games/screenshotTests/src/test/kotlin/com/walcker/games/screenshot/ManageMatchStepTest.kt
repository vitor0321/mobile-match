@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.ui.shared.manageMatch.ManageMatchContent
import com.walcker.games.features.ui.shared.manageMatch.ManageMatchState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class ManageMatchStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        ManageMatchState(
            isLoading = false,
            match = fakeGame(id = "match-1", organizerId = "player-1"),
            currentUserId = "player-1",
        )

    private fun snapshot(
        state: ManageMatchState,
        darkTheme: Boolean = false,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                ManageMatchContent(state = state, strings = PtBrGamesStrings, onEvent = {})
            }
        }
    }

    @Test
    fun loading_lightMode() = snapshot(ManageMatchState())

    @Test
    fun error_lightMode() = snapshot(ManageMatchState(isLoading = false, errorMessage = "Partida não encontrada."))

    @Test
    fun content_lightMode() = snapshot(loadedState)

    @Test
    fun contentWithStats_lightMode() =
        snapshot(
            loadedState.copy(
                match = loadedState.match?.copy(confirmedPlayers = 10, teamCount = 2),
                vipCount = 2,
            ),
        )

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun recurringMatch_lightMode() =
        snapshot(
            loadedState.copy(match = loadedState.match?.copy(recurrence = RecurrenceOption.WEEKLY)),
        )

    @Test
    fun cancelDialog_lightMode() = snapshot(loadedState.copy(showCancelConfirmDialog = true))

    @Test
    fun statusInProgress_lightMode() =
        snapshot(
            loadedState.copy(
                match =
                    loadedState.match?.copy(
                        startsAtSeconds =
                            kotlin.time.Clock.System
                                .now()
                                .epochSeconds - 600,
                        durationMin = 60,
                    ),
            ),
        )

    @Test
    fun teamsNotOfferedForIndividualSport_lightMode() =
        snapshot(
            loadedState.copy(match = loadedState.match?.copy(sport = com.walcker.games.features.domain.shared.model.Sport.NATACAO)),
        )
}
