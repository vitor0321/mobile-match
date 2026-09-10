package com.walcker.games.features.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.ui.home.component.GameList
import com.walcker.games.features.ui.home.map.MatchMapView
import com.walcker.games.features.ui.home.map.component.MapMatchPreviewCard
import com.walcker.games.features.ui.home.map.mapper.toMapPin
import com.walcker.games.features.ui.home.map.model.MatchPreview
import com.walcker.games.features.ui.search.component.SearchFiltersPanel
import com.walcker.games.features.ui.shared.matchDetail.component.Banner
import com.walcker.match.cedar.components.CedarFloatingDialog
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSearchEmptyAnimation
import com.walcker.match.cedar.components.CedarSearchField
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.LocalBottomBarInset
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.MatchDetailCoordinator
import org.koin.compose.koinInject

private val EmptyStateAnimationSize = 240.dp

internal class SearchStep : Screen {
    @Composable
    override fun Content() {
        val matchDetailCoordinator = koinInject<MatchDetailCoordinator>()
        val stepModel = koinScreenModel<SearchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is SearchEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)

                    is SearchEffect.NavigateToMatchDetail ->
                        matchDetailCoordinator.open(effect.matchId)
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose { stepModel.onEvent(SearchEvents.ResetFilters) }
        }

        SearchContent(
            state = state,
            onEvent = stepModel::onEvent,
            snackbarHostState = snackbarHostState,
            mapBody = { bodyModifier ->
                Box(modifier = bodyModifier) {
                    MatchMapView(
                        pins = state.mapResults.map { it.toMapPin() },
                        camera = state.mapCamera,
                        onPinClick = { matchId -> stepModel.onEvent(SearchEvents.PinSelected(matchId)) },
                        onNearbyTap = {},
                        nearbyCount = 0,
                        hasLocationPermission = false,
                        modifier = Modifier.fillMaxSize(),
                        onCameraIdle = { camera -> stepModel.onEvent(SearchEvents.MapCameraIdle(camera)) },
                    )
                    if (state.isMapLoading) {
                        Box(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = CedarTokens.spacing.md),
                        ) {
                            CedarLoading(contentDescription = state.mapStrings.loadingLabel)
                        }
                    }
                    state.mapErrorMessage?.let { message ->
                        Banner(
                            message = message,
                            container = MaterialTheme.colorScheme.errorContainer,
                            onContainer = MaterialTheme.colorScheme.onErrorContainer,
                            dismissContentDescription = state.mapStrings.closeContentDescription,
                            onDismiss = { stepModel.onEvent(SearchEvents.MapErrorDismissed) },
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                    state.previewMatch?.let { game ->
                        MapMatchPreviewCard(
                            preview = MatchPreview(game = game, distanceKm = null),
                            strings = state.mapStrings,
                            gameListStrings = state.cardStrings,
                            onDismiss = { stepModel.onEvent(SearchEvents.MapPreviewDismissed) },
                            onDetailsClick = { stepModel.onEvent(SearchEvents.SelectGame(it)) },
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = CedarTokens.spacing.md,
                                        vertical = CedarTokens.spacing.md,
                                    ).padding(bottom = LocalBottomBarInset.current),
                        )
                    }
                }
            },
        )
    }
}

@Composable
internal fun SearchContent(
    state: SearchState,
    onEvent: (SearchEvents) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    mapBody: @Composable (Modifier) -> Unit = {},
) {
    val strings = state.strings
    val cardStrings = state.cardStrings

    if (state.showFiltersPanel) {
        CedarFloatingDialog(
            onDismiss = { onEvent(SearchEvents.ToggleFiltersPanel) },
            scrollable = false,
        ) {
            SearchFiltersPanel(
                strings = strings,
                selectedSports = state.filters.sports,
                mySports = state.mySports,
                startDateMs = state.filters.startDateMs,
                endDateMs = state.filters.endDateMs,
                minPrice = state.filters.minPrice,
                maxPrice = state.filters.maxPrice,
                onSportToggled = { sport ->
                    val current = state.filters.sports
                    val next =
                        if (sport == null) {
                            emptySet()
                        } else if (sport in current) {
                            current - sport
                        } else {
                            current + sport
                        }
                    onEvent(SearchEvents.SportFilterChanged(next))
                },
                onDateRangeChanged = { start, end ->
                    onEvent(SearchEvents.DateRangeChanged(start, end))
                },
                onPriceRangeChanged = { min, max ->
                    onEvent(SearchEvents.PriceRangeChanged(min, max))
                },
                onResetFilters = { onEvent(SearchEvents.ResetFilters) },
                onDismiss = { onEvent(SearchEvents.ToggleFiltersPanel) },
            )
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = CedarTokens.colors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = CedarTokens.spacing.lg,
                        vertical = CedarTokens.spacing.md,
                    ),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            ) {
                CedarScreenTitle(title = strings.title, subtitle = strings.subtitle)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CedarSearchField(
                        value = state.query,
                        onValueChange = { onEvent(SearchEvents.QueryChanged(it)) },
                        placeholder = strings.placeholder,
                        clearContentDescription = strings.clearQueryContentDescription,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onEvent(SearchEvents.ToggleFiltersPanel) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = strings.openFiltersContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { onEvent(SearchEvents.ToggleMap) },
                    ) {
                        Icon(
                            imageVector = if (state.showMap) Icons.AutoMirrored.Filled.List else Icons.Filled.Map,
                            contentDescription = if (state.showMap) strings.showListAction else strings.showMapAction,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            val hasActiveFilters = state.filters != SearchFilters()
            when {
                state.isLoading ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CedarLoading(contentDescription = strings.loadingLabel)
                    }

                state.errorMessage != null ->
                    EmptyState(
                        message = state.errorMessage ?: strings.loadErrorMessage,
                        actionLabel = strings.retry,
                        onAction = { onEvent(SearchEvents.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )

                state.showMap -> mapBody(Modifier.fillMaxSize())

                state.query.isBlank() && !hasActiveFilters && state.results.isEmpty() ->
                    EmptyState(
                        message = strings.idlePrompt,
                        modifier = Modifier.fillMaxSize(),
                    )

                state.results.isEmpty() ->
                    EmptyState(
                        message =
                            if (state.query.isNotBlank()) {
                                strings.emptyForQuery(state.query)
                            } else {
                                strings.emptyForFilters
                            },
                        illustration = {
                            CedarSearchEmptyAnimation(
                                contentDescription = null,
                                modifier = Modifier.size(EmptyStateAnimationSize),
                            )
                        },
                        actionLabel = strings.clearFilters,
                        onAction = { onEvent(SearchEvents.ResetFilters) },
                        modifier = Modifier.fillMaxSize(),
                    )

                else -> {
                    Text(
                        text = strings.resultsCount(state.results.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.padding(
                                horizontal = CedarTokens.spacing.lg,
                                vertical = CedarTokens.spacing.xs,
                            ),
                    )
                    GameList(
                        strings = cardStrings,
                        games = state.visibleResults,
                        onClick = { onEvent(SearchEvents.SelectGame(it)) },
                        hasMore = state.hasMoreResults,
                        onLoadMore = { onEvent(SearchEvents.LoadMoreResults) },
                    )
                }
            }
        }
    }
}
