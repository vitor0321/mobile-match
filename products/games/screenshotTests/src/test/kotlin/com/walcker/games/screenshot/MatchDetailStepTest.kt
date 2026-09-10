@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.MatchStatus
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

    private val fullMatchState =
        MatchDetailState(
            isLoading = false,
            match =
                fakeGame(
                    id = "match-2",
                    confirmedPlayers = 10,
                    totalPlayers = 10,
                    status = MatchStatus.FULL,
                    participants = listOf("player-2"),
                ),
            participants =
                fakeParticipantsSummary(
                    confirmed = listOf(fakeParticipant(userId = "player-2")),
                    confirmedCount = 10,
                    totalSlots = 10,
                ),
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
    fun joiningWaitlist_lightMode() = snapshot(fullMatchState.copy(isJoining = true), darkTheme = false)

    @Test
    fun joiningWaitlist_darkMode() = snapshot(fullMatchState.copy(isJoining = true), darkTheme = true)

    @Test
    fun organizerView_lightMode() =
        snapshot(
            loadedState.copy(
                match = loadedState.match?.copy(organizerId = "player-1", participants = listOf("player-1")),
            ),
            darkTheme = false,
        )

    @Test
    fun actionError_lightMode() =
        snapshot(
            loadedState.copy(actionErrorMessage = "Não foi possível sair da partida. Tente de novo."),
            darkTheme = false,
        )

    @Test
    fun yourStatusWaitlist_lightMode() =
        snapshot(
            loadedState.copy(
                participants =
                    fakeParticipantsSummary(
                        confirmed = emptyList(),
                        waitlist = listOf(fakeParticipant(userId = "player-1", isConfirmed = false, positionInWaitlist = 2)),
                        confirmedCount = 0,
                    ),
            ),
            darkTheme = false,
        )

    @Test
    fun yourTeamAssignment_lightMode() =
        snapshot(
            loadedState.copy(
                match = loadedState.match?.copy(teamCount = 2, teamAssignments = mapOf("player-1" to 1)),
            ),
            darkTheme = false,
        )

    @Test
    fun yourStatusWaitlist_darkMode() =
        snapshot(
            loadedState.copy(
                participants =
                    fakeParticipantsSummary(
                        confirmed = emptyList(),
                        waitlist = listOf(fakeParticipant(userId = "player-1", isConfirmed = false, positionInWaitlist = 2)),
                        confirmedCount = 0,
                    ),
            ),
            darkTheme = true,
        )

    @Test
    fun organizerNotParticipating_lightMode() =
        snapshot(
            loadedState.copy(
                match = loadedState.match?.copy(organizerId = "player-1", participants = emptyList()),
                participants = fakeParticipantsSummary(confirmed = emptyList()),
            ),
            darkTheme = false,
        )
}
