@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.create.CreateMatchContent
import com.walcker.games.features.ui.create.CreateMatchState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class CreateMatchStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val filledState =
        CreateMatchState(
            venueName = "Quadra Central",
            selectedSport = Sport.FUTSAL,
            lat = -23.55,
            lng = -46.63,
            address = "Rua Um, 100",
            neighborhood = "Centro",
            city = "São Paulo",
            selectedDate = 4_102_444_800_000L,
            selectedTime = 19 to 0,
        )

    private fun snapshot(
        state: CreateMatchState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                CreateMatchContent(state = state, onEvent = {}, strings = PtBrGamesStrings.createMatch)
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(filledState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(filledState, darkTheme = true)

    @Test
    fun empty_lightMode() = snapshot(CreateMatchState(), darkTheme = false)

    @Test
    fun editMode_lightMode() = snapshot(filledState.copy(isEditMode = true), darkTheme = false)

    @Test
    fun loading_lightMode() = snapshot(CreateMatchState(isLoading = true), darkTheme = false)
}
