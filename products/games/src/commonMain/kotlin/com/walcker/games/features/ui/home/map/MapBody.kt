package com.walcker.games.features.ui.home.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.walcker.games.features.ui.home.map.component.LocationUnavailableCard
import com.walcker.games.features.ui.home.map.component.NearbyMatchesSheet
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.tokens.CedarTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapBody(
    state: MapState,
    onRefresh: () -> Unit,
    onRetryLocation: () -> Unit,
    onPinClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = rememberGamesStrings().strings.map
    var showNearbySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = modifier) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            MatchMapView(
                pins = state.pins,
                camera = state.camera,
                onPinClick = onPinClick,
                onNearbyTap = { showNearbySheet = true },
                nearbyCount = state.nearbyMatches.size,
                hasLocationPermission = state.hasLocationPermission,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (state.isLoading) {
            CedarLoading(
                contentDescription = strings.loadingLabel,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (state.locationUnavailable) {
            LocationUnavailableCard(
                strings = strings,
                onRetry = onRetryLocation,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(CedarTokens.spacing.md),
            )
        }
    }

    if (showNearbySheet && state.nearbyMatches.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showNearbySheet = false },
            sheetState = sheetState,
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NearbyMatchesSheet(
                strings = strings,
                matches = state.nearbyMatches,
                onMatchTap = { matchId ->
                    showNearbySheet = false
                    onPinClick(matchId)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
