@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.create.locationPicker.LocationPickerContent
import com.walcker.games.features.ui.create.locationPicker.LocationPickerState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class LocationPickerStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val resolvedState =
        LocationPickerState(
            lat = -23.55,
            lng = -46.63,
            address = "Rua Um, 100",
            neighborhood = "Centro",
            city = "São Paulo",
            isResolvingLocation = false,
        )

    private fun snapshot(
        state: LocationPickerState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                LocationPickerContent(state = state, strings = PtBrGamesStrings.createMatch)
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(resolvedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(resolvedState, darkTheme = true)

    @Test
    fun resolvingLocation_lightMode() = snapshot(LocationPickerState(lat = -23.55, lng = -46.63), darkTheme = false)

    @Test
    fun searchError_lightMode() = snapshot(resolvedState.copy(addressQuery = "endereço inexistente", searchError = true), darkTheme = false)
}
