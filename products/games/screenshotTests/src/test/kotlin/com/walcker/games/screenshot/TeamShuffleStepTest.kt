@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.shared.teamShuffle.TeamShuffleContent
import com.walcker.games.features.ui.shared.teamShuffle.TeamShuffleState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class TeamShuffleStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        TeamShuffleState(
            isLoading = false,
            match = fakeGame(id = "match-1", organizerId = "player-1"),
        )

    private fun snapshot(
        state: TeamShuffleState,
        darkTheme: Boolean = false,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                TeamShuffleContent(state = state, manage = PtBrGamesStrings.manageMatch, onEvent = {})
            }
        }
    }

    @Test
    fun loading_lightMode() = snapshot(TeamShuffleState())

    @Test
    fun selectorsOnly_lightMode() = snapshot(loadedState.copy(selectedTeamCount = 3, selectedPlayersPerTeam = 4))

    @Test
    fun selectorsOnly_darkMode() = snapshot(loadedState.copy(selectedTeamCount = 3, selectedPlayersPerTeam = 4), darkTheme = true)

    @Test
    fun teamsFormed_lightMode() =
        snapshot(
            loadedState.copy(
                match =
                    loadedState.match?.copy(
                        teamCount = 2,
                        playersPerTeam = 2,
                        teamAssignments = mapOf("player-2" to 0, "player-3" to 1),
                    ),
                confirmedPlayers =
                    listOf(
                        fakeParticipant(userId = "player-2", displayName = "Bruno Lima"),
                        fakeParticipant(userId = "player-3", displayName = "Carla Dias"),
                    ),
            ),
        )

    @Test
    fun teamsWithUnassignedPlayer_lightMode() =
        snapshot(
            loadedState.copy(
                match =
                    loadedState.match?.copy(
                        teamCount = 2,
                        playersPerTeam = 1,
                        teamAssignments = mapOf("player-2" to 0),
                    ),
                confirmedPlayers =
                    listOf(
                        fakeParticipant(userId = "player-2", displayName = "Bruno Lima"),
                        fakeParticipant(userId = "player-3", displayName = "Carla Dias"),
                    ),
            ),
        )
}
