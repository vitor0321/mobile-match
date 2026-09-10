@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
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
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun recurringMatch_lightMode() =
        snapshot(
            loadedState.copy(match = loadedState.match?.copy(recurrence = RecurrenceOption.WEEKLY)),
        )

    @Test
    fun ratePlayersEmpty_lightMode() =
        snapshot(loadedState.copy(canRatePlayers = true))

    @Test
    fun ratePlayers_lightMode() =
        snapshot(
            loadedState.copy(
                canRatePlayers = true,
                confirmedPlayers = listOf(fakeParticipant(userId = "player-2", displayName = "Bruno Lima")),
                participantRatings = mapOf("player-2" to PlayerRatingSummary(rating = 4.5f, ratingCount = 3)),
            ),
        )

    @Test
    fun cancelDialog_lightMode() = snapshot(loadedState.copy(showCancelConfirmDialog = true))

    @Test
    fun teamsSelector_lightMode() =
        snapshot(loadedState.copy(selectedTeamCount = 3))

    @Test
    fun teamsFormed_lightMode() =
        snapshot(
            loadedState.copy(
                match =
                    loadedState.match?.copy(
                        teamCount = 2,
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
                match = loadedState.match?.copy(teamCount = 2, teamAssignments = mapOf("player-2" to 0)),
                confirmedPlayers =
                    listOf(
                        fakeParticipant(userId = "player-2", displayName = "Bruno Lima"),
                        fakeParticipant(userId = "player-3", displayName = "Carla Dias"),
                    ),
            ),
        )

    @Test
    fun teamsNotOfferedForIndividualSport_lightMode() =
        snapshot(
            loadedState.copy(match = loadedState.match?.copy(sport = com.walcker.games.features.domain.shared.model.Sport.NATACAO)),
        )
}
