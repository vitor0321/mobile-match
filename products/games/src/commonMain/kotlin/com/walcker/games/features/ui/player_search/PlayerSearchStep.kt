package com.walcker.games.features.ui.player_search

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
import androidx.compose.material3.CircularProgressIndicator
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
import com.walcker.games.features.ui.player_details.PlayerDetailsStep
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSearchField
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens

/**
 * Player search — find other players by name, rating and sport.
 */
internal class PlayerSearchStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
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

        if (state.showFiltersPanel) {
            ModalBottomSheet(
                onDismissRequest = { stepModel.onEvent(PlayerSearchEvents.ToggleFiltersPanel) },
                shape = CedarTokens.radius.sheet,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                PlayerFiltersPanel(
                    filters = state.filters,
                    strings = strings,
                    onFiltersChanged = { filters ->
                        stepModel.onEvent(PlayerSearchEvents.FiltersChanged(filters))
                    },
                    onResetFilters = { stepModel.onEvent(PlayerSearchEvents.ResetFilters) },
                    onDismiss = { stepModel.onEvent(PlayerSearchEvents.ToggleFiltersPanel) },
                )
            }
        }

        Scaffold(
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
                            onValueChange = {
                                stepModel.onEvent(PlayerSearchEvents.QueryChanged(it))
                            },
                            placeholder = strings.placeholder,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { stepModel.onEvent(PlayerSearchEvents.ToggleFiltersPanel) },
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
                    state.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    state.errorMessage != null -> EmptyState(
                        message = state.errorMessage ?: strings.errorLoading,
                        actionLabel = strings.retry,
                        onAction = {
                            stepModel.onEvent(PlayerSearchEvents.QueryChanged(state.query))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Nothing typed yet. Not the same as "we looked and found nobody".
                    state.isIdle -> EmptyState(
                        message = strings.emptySearchPrompt,
                        modifier = Modifier.fillMaxSize(),
                    )

                    state.results.isEmpty() -> EmptyState(
                        message = strings.emptyForQuery(state.query),
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = CedarTokens.spacing.lg,
                            vertical = CedarTokens.spacing.xs,
                        ),
                        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                    ) {
                        // The list may be missing people the query never read. Say so
                        // rather than let it look complete.
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
                                ratingAccessibilityLabel = strings.ratingAccessibility(
                                    player.averageRating,
                                ),
                                onPlayerSelected = { userId ->
                                    stepModel.onEvent(PlayerSearchEvents.SelectPlayer(userId))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
