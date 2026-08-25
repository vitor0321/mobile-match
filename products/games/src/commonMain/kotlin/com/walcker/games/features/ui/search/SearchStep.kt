package com.walcker.games.features.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.features.ui.matchdetail.MatchDetailStep
import com.walcker.games.strings.SearchStrings
import com.walcker.match.cedar.components.CedarFilterRow
import com.walcker.match.cedar.components.CedarFilterSection
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSearchField
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens

/**
 * Search screen — filters the local cache by venue, neighborhood, city or sport
 * label. Search runs entirely client-side; the cache is updated whenever the home
 * screen refreshes.
 *
 * O `Step` só liga o model à UI: assina o estado, coleta efeitos e navega.
 */
internal class SearchStep : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<SearchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is SearchEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)

                    is SearchEffect.NavigateToMatchDetail ->
                        navigator.push(MatchDetailStep(effect.matchId))
                }
            }
        }

        SearchContent(
            state = state,
            onEvent = stepModel::onEvent,
            snackbarHostState = snackbarHostState,
        )
    }
}

/** Conteúdo da busca, stateless: recebe estado e devolve eventos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchContent(
    state: SearchState,
    onEvent: (SearchEvents) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
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
                onSportToggled = { sport ->
                    val current = state.filters.sports
                    val next = if (sport == null) {
                        emptySet()
                    } else if (sport in current) {
                        current - sport
                    } else {
                        current + sport
                    }
                    onEvent(SearchEvents.SportFilterChanged(next))
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier.padding(
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
                }
            }

            when {
                // Nothing typed yet. This is not an empty result — saying "no
                // matches found" before the user has searched reads as a failure.
                state.query.isBlank() && state.results.isEmpty() ->
                    EmptyState(
                        message = strings.idlePrompt,
                        modifier = Modifier.fillMaxSize(),
                    )

                state.results.isEmpty() ->
                    EmptyState(
                        message = strings.emptyForQuery(state.query),
                        actionLabel = strings.clearFilters,
                        onAction = { onEvent(SearchEvents.ResetFilters) },
                        modifier = Modifier.fillMaxSize(),
                    )

                else -> {
                    Text(
                        text = strings.resultsCount(state.results.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = CedarTokens.spacing.lg,
                            vertical = CedarTokens.spacing.xs,
                        ),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = CedarTokens.spacing.lg,
                            vertical = CedarTokens.spacing.xs,
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
                                onClick = { onEvent(SearchEvents.SelectGame(game.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The filter sheet.
 *
 * Sport is the only filter with real behaviour today — it is multi-select and
 * applies as you tap. Date and price exist in [SearchFilters] but have no UI, so
 * they are rendered disabled rather than hidden: a filter you cannot see is a
 * filter you cannot ask for.
 */
@Composable
private fun SearchFiltersPanel(
    strings: SearchStrings,
    selectedSports: Set<Sport>,
    onSportToggled: (Sport?) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.lg),
    ) {
        Text(
            text = strings.filtersTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        CedarFilterSection(label = strings.filterSport) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs)) {
                item {
                    SportChip(
                        label = strings.allSports,
                        selected = selectedSports.isEmpty(),
                        onClick = { onSportToggled(null) },
                    )
                }
                items(items = Sport.entries) { sport ->
                    SportChip(
                        label = sport.label,
                        selected = sport in selectedSports,
                        onClick = { onSportToggled(sport) },
                    )
                }
            }
        }

        CedarFilterRow(
            label = strings.filterDate,
            value = null,
            placeholder = strings.comingSoon,
            onClick = {},
            enabled = false,
        )

        CedarFilterRow(
            label = strings.filterPrice,
            value = null,
            placeholder = strings.comingSoon,
            onClick = {},
            enabled = false,
        )

        CedarPrimaryButton(
            text = strings.applyFilters,
            onClick = onDismiss,
        )
        CedarTextButton(
            text = strings.clearFilters,
            onClick = onResetFilters,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
