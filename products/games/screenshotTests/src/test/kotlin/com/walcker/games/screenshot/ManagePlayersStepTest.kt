@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.ui.shared.managePlayers.ManagePlayersContent
import com.walcker.games.features.ui.shared.managePlayers.ManagePlayersState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class ManagePlayersStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        ManagePlayersState(
            isLoading = false,
            match = fakeGame(id = "match-1", organizerId = "player-1"),
            currentUserId = "player-1",
            canManage = true,
        )

    private fun snapshot(
        state: ManagePlayersState,
        darkTheme: Boolean = false,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                ManagePlayersContent(state = state, strings = PtBrGamesStrings, onEvent = {})
            }
        }
    }

    @Test
    fun loading_lightMode() = snapshot(ManagePlayersState())

    @Test
    fun error_lightMode() = snapshot(ManagePlayersState(isLoading = false, errorMessage = "Partida não encontrada."))

    @Test
    fun empty_lightMode() = snapshot(loadedState)

    @Test
    fun empty_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun withPlayers_lightMode() =
        snapshot(
            loadedState.copy(
                confirmedPlayers = listOf(fakeParticipant(userId = "player-2", displayName = "Bruno Lima")),
                participantRatings = mapOf("player-2" to PlayerRatingSummary(rating = 4.5f, ratingCount = 3)),
            ),
        )

    @Test
    fun withPlayersAndWaitlist_lightMode() =
        snapshot(
            loadedState.copy(
                match = loadedState.match?.copy(seriesId = "series-1"),
                confirmedPlayers = listOf(fakeParticipant(userId = "player-2", displayName = "Bruno Lima").copy(isVip = true)),
                participantRatings = mapOf("player-2" to PlayerRatingSummary(rating = 4.5f, ratingCount = 3)),
                waitlistPlayers =
                    listOf(
                        fakeParticipant(userId = "player-3", displayName = "Carla Dias", positionInWaitlist = 1),
                    ),
            ),
        )

    @Test
    fun withPlayersAndWaitlist_darkMode() =
        snapshot(
            loadedState.copy(
                match = loadedState.match?.copy(seriesId = "series-1"),
                confirmedPlayers = listOf(fakeParticipant(userId = "player-2", displayName = "Bruno Lima").copy(isVip = true)),
                participantRatings = mapOf("player-2" to PlayerRatingSummary(rating = 4.5f, ratingCount = 3)),
                waitlistPlayers =
                    listOf(
                        fakeParticipant(userId = "player-3", displayName = "Carla Dias", positionInWaitlist = 1),
                    ),
            ),
            darkTheme = true,
        )

    @Test
    fun banConfirmDialog_lightMode() =
        snapshot(
            loadedState.copy(
                confirmedPlayers = listOf(fakeParticipant(userId = "player-2", displayName = "Bruno Lima")),
                playerPendingBan = "player-2" to "Bruno Lima",
            ),
        )
}
