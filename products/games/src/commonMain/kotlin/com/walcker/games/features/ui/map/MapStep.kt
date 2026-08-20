package com.walcker.games.features.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.match.core.geo.formatDistance
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import org.koin.compose.koinInject

/**
 * Map view showing open matches as live pins.
 *
 * Tapping a pin emits a [DeepLink.OpenMatch] through the [DeepLinkCoordinator],
 * which the navigation shell catches to open the match detail overlay — same
 * path used by notification taps.
 *
 * Includes pull-to-refresh and a bottom sheet with nearby matches sorted by distance.
 */
internal class MapStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<MapStepModel>()
        val deepLinkCoordinator: DeepLinkCoordinator = koinInject()
        val state by stepModel.state.collectAsState()
        var showNearbySheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        Box(modifier = Modifier.fillMaxSize()) {
            // PullToRefreshBox do Material3, e não o SwipeRefresh do
            // Accompanist: Accompanist é biblioteca Android, e este arquivo
            // está em commonMain — o alvo iOS não compilava por causa desses
            // dois imports.
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { stepModel.onRefresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                MatchMapView(
                    pins = state.pins,
                    camera = state.camera,
                    onPinClick = { matchId ->
                        deepLinkCoordinator.navigate(DeepLink.OpenMatch(matchId))
                    },
                    onNearbyTap = { showNearbySheet = true },
                    nearbyCount = state.nearbyMatches.size,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Sem posição, "partidas próximas" fica vazio para sempre e a
            // câmera não sai da Paulista. Dizer isso é melhor do que deixar a
            // pessoa achar que não há partida perto dela.
            if (state.locationUnavailable) {
                LocationUnavailableCard(
                    onRetry = { stepModel.onRetryLocation() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }

        // Bottom sheet with nearby matches
        if (showNearbySheet && state.nearbyMatches.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { showNearbySheet = false },
                sheetState = sheetState,
            ) {
                NearbyMatchesSheet(
                    matches = state.nearbyMatches,
                    onMatchTap = { matchId ->
                        showNearbySheet = false
                        deepLinkCoordinator.navigate(DeepLink.OpenMatch(matchId))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LocationUnavailableCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sem acesso à sua localização",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "O mapa abre em São Paulo e a lista de partidas próximas " +
                    "fica vazia até você liberar a localização.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRetry) {
                Text("Tentar de novo")
            }
        }
    }
}

@Composable
private fun NearbyMatchesSheet(
    matches: List<NearbyMatch>,
    onMatchTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Partidas Próximas (${matches.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = matches, key = { it.game.id }) { nearby ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nearby.game.venueName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = nearby.game.sport.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = formatDistance(nearby.distanceKm),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Spacer for bottom padding when used in bottom sheet
        Box(modifier = Modifier.padding(bottom = 32.dp))
    }
}
