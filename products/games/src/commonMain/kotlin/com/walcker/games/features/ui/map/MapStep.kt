package com.walcker.games.features.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import org.koin.compose.koinInject

/**
 * Map view showing open matches as live pins.
 *
 * Tapping a pin emits a [DeepLink.OpenMatch] through the [DeepLinkCoordinator],
 * which the navigation shell catches to open the match detail overlay — same
 * path used by notification taps.
 */
internal class MapStep : Screen {

    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<MapStepModel>()
        val deepLinkCoordinator: DeepLinkCoordinator = koinInject()
        val state by stepModel.state.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            MatchMapView(
                pins = state.pins,
                camera = state.camera,
                onPinClick = { matchId ->
                    deepLinkCoordinator.navigate(DeepLink.OpenMatch(matchId))
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
