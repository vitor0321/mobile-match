@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.DimensionAverage
import com.walcker.games.features.domain.shared.model.RatingDimension
import com.walcker.games.features.ui.shared.playerDetails.PlayerDetailsContent
import com.walcker.games.features.ui.shared.playerDetails.PlayerDetailsState
import com.walcker.games.strings.PtBrGamesStrings
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test

class PlayerDetailsStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val player =
        fakePlayerDetails(
            dimensionAverages =
                mapOf(
                    RatingDimension.PUNCTUALITY to DimensionAverage(average = 4.5f, count = 10),
                    RatingDimension.RESPECT to DimensionAverage(average = 4.8f, count = 10),
                    RatingDimension.FAIR_PLAY to DimensionAverage(average = 4.2f, count = 10),
                    RatingDimension.BEHAVIOR to DimensionAverage(average = 4.9f, count = 10),
                ),
        )

    private val loadedState =
        PlayerDetailsState(
            userId = player.userId,
            player = player,
            previewRatings = persistentListOf(fakeRating(id = "1"), fakeRating(id = "2")),
            distribution = fakeRatingDistribution(),
            hasMoreRatings = true,
        )

    private fun snapshot(
        state: PlayerDetailsState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                PlayerDetailsContent(
                    state = state,
                    player = state.player ?: player,
                    strings = PtBrGamesStrings.playerDetails,
                    ratingStrings = PtBrGamesStrings.ratings,
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(loadedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun noReviews_lightMode() = snapshot(PlayerDetailsState(userId = player.userId, player = player), darkTheme = false)
}
