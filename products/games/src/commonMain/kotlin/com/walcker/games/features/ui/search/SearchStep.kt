package com.walcker.games.features.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.walcker.games.features.ui.home.map.MatchMapView
import com.walcker.games.features.ui.home.map.component.MapMatchPreviewCard
import com.walcker.games.features.ui.home.map.mapper.toMapPin
import com.walcker.games.features.ui.home.map.model.MatchPreview
import com.walcker.games.features.ui.search.component.SearchFiltersPanel
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSearchEmptyAnimation
import com.walcker.match.cedar.components.CedarSearchField
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.LocalBottomBarInset
import com.walcker.match.cedar.components.MatchCard
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
                        pins = state.results.map { it.toMapPin() },
                        camera = state.mapCamera,
                        onPinClick = { matchId -> stepModel.onEvent(SearchEvents.PinSelected(matchId)) },
                        onNearbyTap = {},
                        nearbyCount = 0,
                        hasLocationPermission = false,
                        modifier = Modifier.fillMaxSize(),
                    )
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
                                    ),
                        )
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        ModalBottomSheet(
            onDismissRequest = { onEvent(SearchEvents.ToggleFiltersPanel) },
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                start = CedarTokens.spacing.lg,
                                end = CedarTokens.spacing.lg,
                                top = CedarTokens.spacing.xs,
                                bottom = CedarTokens.spacing.xs + LocalBottomBarInset.current,
                            ),
                        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                    ) {
                        items(items = state.results, key = { it.id }) { game ->
                            MatchCard(
                                venueName = game.venueName,
                                startsAtSeconds = game.startsAtSeconds,
                                metaLabel = "${game.sport.label} · ${game.neighborhood}",
                                priceLabel = game.pricePerPlayer?.let { cardStrings.perPlayer(it) },
                                slotsLabel = cardStrings.slotsBadge(game.openSlots),
                                openSlots = game.openSlots,
                                cityLabel = game.city,
                                onClick = { onEvent(SearchEvents.SelectGame(game.id)) },
                                matchRating = game.matchRating.toFloat().takeIf { game.matchRatingCount > 0 },
                                matchRatingCountLabel =
                                    cardStrings.ratingsCount(game.matchRatingCount).takeIf { game.matchRatingCount > 0 },
                            )
                        }
                    }
                }
            }
        }
    }
}
