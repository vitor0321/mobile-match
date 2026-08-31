package com.walcker.games.features.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.ui.search.SearchStep
import com.walcker.games.strings.MapStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarSearchEntryPoint
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.geo.formatDistance
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import com.walcker.match.navigator.HomeViewCoordinator
import org.koin.compose.koinInject

internal class MapStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<MapStepModel>()
        val deepLinkCoordinator: DeepLinkCoordinator = koinInject()
        val homeViewCoordinator: HomeViewCoordinator = koinInject()
        val state by stepModel.state.collectAsState()
        val strings = rememberGamesStrings().strings.map
        var showNearbySheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        Box(modifier = Modifier.fillMaxSize()) {
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
                    hasLocationPermission = state.hasLocationPermission,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            CedarSearchEntryPoint(
                placeholder = strings.searchPlaceholder,
                onClick = { navigator.push(SearchStep()) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(CedarTokens.spacing.md),
            )

            Surface(
                shape = CedarTokens.radius.smShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(CedarTokens.spacing.md),
            ) {
                IconButton(onClick = { homeViewCoordinator.setShowMap(false) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = strings.showListAction,
                    )
                }
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
                    onRetry = { stepModel.onRetryLocation() },
                    modifier = Modifier
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
    strings: MapStrings,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.lgShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.overlay),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        ) {
            Text(
                text = strings.locationUnavailableTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = strings.locationUnavailableBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CedarTextButton(text = strings.retry, onClick = onRetry)
        }
    }
}

@Composable
private fun NearbyMatchesSheet(
    strings: MapStrings,
    matches: List<NearbyMatch>,
    onMatchTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CedarSectionHeader(
            title = strings.nearbyTitle,
            subtitle = strings.nearbySubtitle(matches.size),
            modifier = Modifier.padding(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.sm,
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = CedarTokens.spacing.lg,
                end = CedarTokens.spacing.lg,
                top = CedarTokens.spacing.xs,
                bottom = CedarTokens.spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            items(items = matches, key = { it.game.id }) { nearby ->
                MatchCard(
                    venueName = nearby.game.venueName,
                    startsAtSeconds = nearby.game.startsAtSeconds,
                    metaLabel = "${nearby.game.sport.label} · ${formatDistance(nearby.distanceKm)}",
                    onClick = { onMatchTap(nearby.game.id) },
                )
            }
        }
    }
}
