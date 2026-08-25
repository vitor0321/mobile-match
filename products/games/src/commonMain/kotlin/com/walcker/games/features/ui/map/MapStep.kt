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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.walcker.match.cedar.components.CedarSearchEntryPoint
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.geo.formatDistance
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import org.koin.compose.koinInject

/**
 * Map view showing open matches as live pins.
 *
 * Tapping a pin emits a [DeepLink.OpenMatch] through the [DeepLinkCoordinator],
 * which the navigation shell catches to open the match detail overlay — same path
 * used by notification taps.
 *
 * The redesign adds the search bar floating over the map. It is the first thing on
 * the Figma's home screen and it was missing entirely: the only way into search was
 * the bottom tab, which meant the map had no way to answer "not here — over there".
 */
internal class MapStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<MapStepModel>()
        val deepLinkCoordinator: DeepLinkCoordinator = koinInject()
        val state by stepModel.state.collectAsState()
        val strings = rememberGamesStrings().strings.map
        var showNearbySheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        Box(modifier = Modifier.fillMaxSize()) {
            // PullToRefreshBox do Material3, e não o SwipeRefresh do Accompanist:
            // Accompanist é biblioteca Android, e este arquivo está em commonMain —
            // o alvo iOS não compilava por causa desses dois imports.
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

            CedarSearchEntryPoint(
                placeholder = strings.searchPlaceholder,
                onClick = { navigator.push(SearchStep()) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(CedarTokens.spacing.md),
            )

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Sem posição, "partidas próximas" fica vazio para sempre e a câmera não
            // sai da Paulista. Dizer isso é melhor do que deixar a pessoa achar que
            // não há partida perto dela.
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

/**
 * "Partidas perto de você" — the sheet the map opens onto.
 *
 * Uses the same [MatchCard] as every other list. It used to render its own row
 * layout, which is why a match looked like one thing on the map and another thing
 * in search — and why the sport showed as `FUTEBOL` here and `Futebol` everywhere
 * else: this was the one call site reading `sport.name` instead of `sport.label`.
 */
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
