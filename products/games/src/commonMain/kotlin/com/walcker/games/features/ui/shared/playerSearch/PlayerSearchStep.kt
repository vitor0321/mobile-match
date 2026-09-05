package com.walcker.games.features.ui.shared.playerSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.walcker.games.features.ui.shared.playerDetails.PlayerDetailsStep
import com.walcker.games.features.ui.shared.playerSearch.component.PlayerFiltersPanel
import com.walcker.games.features.ui.shared.playerSearch.component.PlayerSearchResultCard
import com.walcker.games.strings.PlayerSearchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSearchField
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.LocalBottomBarInset
import com.walcker.match.cedar.tokens.CedarTokens

internal class PlayerSearchStep : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<PlayerSearchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val strings = rememberGamesStrings().strings.playerSearch

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is PlayerSearchEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)
                    is PlayerSearchEffect.NavigateToPlayer ->
                        navigator.push(PlayerDetailsStep(effect.userId))
                }
            }
        }

        PlayerSearchContent(
            state = state,
            onEvent = stepModel::onEvent,
            strings = strings,
            snackbarHostState = snackbarHostState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSearchContent(
    state: PlayerSearchState,
    onEvent: (PlayerSearchEvents) -> Unit,
    strings: PlayerSearchStrings,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    if (state.showFiltersPanel) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(PlayerSearchEvents.ToggleFiltersPanel) },
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            PlayerFiltersPanel(
                filters = state.filters,
                strings = strings,
                onFiltersChanged = { filters ->
                    onEvent(PlayerSearchEvents.FiltersChanged(filters))
                },
                onResetFilters = { onEvent(PlayerSearchEvents.ResetFilters) },
                onDismiss = { onEvent(PlayerSearchEvents.ToggleFiltersPanel) },
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
                        onValueChange = {
                            onEvent(PlayerSearchEvents.QueryChanged(it))
                        },
                        placeholder = strings.placeholder,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onEvent(PlayerSearchEvents.ToggleFiltersPanel) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = strings.filtersButton,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

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
                        message = state.errorMessage ?: strings.errorLoading,
                        actionLabel = strings.retry,
                        onAction = {
                            onEvent(PlayerSearchEvents.QueryChanged(state.query))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                state.isIdle ->
                    EmptyState(
                        message = strings.emptySearchPrompt,
                        modifier = Modifier.fillMaxSize(),
                    )

                state.results.isEmpty() ->
                    EmptyState(
                        message = strings.emptyForQuery(state.query),
                        modifier = Modifier.fillMaxSize(),
                    )

                else ->
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
                        if (state.reachedLimit) {
                            item(key = "reached-limit") {
                                Text(
                                    text = strings.reachedLimit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(items = state.results, key = { it.userId }) { player ->
                            PlayerSearchResultCard(
                                player = player,
                                ratingLabel = strings.ratingValue(player.averageRating),
                                ratingAccessibilityLabel =
                                    strings.ratingAccessibility(
                                        player.averageRating,
                                    ),
                                onPlayerSelected = { userId ->
                                    onEvent(PlayerSearchEvents.SelectPlayer(userId))
                                },
                            )
                        }
                    }
            }
        }
    }
}
