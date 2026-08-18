package com.walcker.games.features.ui.player_search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.ui.player_details.PlayerDetailsStep
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.EmptyState
import kotlinx.collections.immutable.persistentListOf

/**
 * Player search screen — allows users to search for other players
 * by name, rating, sports, and match experience.
 *
 * Presents as 6th tab in main navigation.
 */
internal class PlayerSearchStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<PlayerSearchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val gamesStrings = rememberGamesStrings().strings
        val strings = gamesStrings.playerSearch

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
            ) {
                PlayerFiltersPanel(
                    filters = state.filters,
                    strings = strings,
                    onFiltersChanged = { filters ->
                        stepModel.onEvent(PlayerSearchEvents.FiltersChanged(filters))
                    },
                    onResetFilters = {
                        stepModel.onEvent(PlayerSearchEvents.ResetFilters)
                    },
                    onDismiss = {
                        stepModel.onEvent(PlayerSearchEvents.ToggleFiltersPanel)
                    },
                )
            }
        }

        Scaffold(
            topBar = {
                CedarTopBar(
                    title = strings.title,
                    subtitle = strings.subtitle,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Search bar with filter button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { stepModel.onEvent(PlayerSearchEvents.QueryChanged(it)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(strings.placeholder) },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = { stepModel.onEvent(PlayerSearchEvents.ToggleFiltersPanel) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = strings.filtersButton,
                        )
                    }
                }

                // Results or loading/empty state
                when {
                    state.isLoading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    state.errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = state.errorMessage ?: strings.errorLoading,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = {
                                    stepModel.onEvent(
                                        PlayerSearchEvents.QueryChanged(state.query),
                                    )
                                },
                                modifier = Modifier.padding(top = 16.dp),
                            ) {
                                Text(strings.retry)
                            }
                        }
                    }
                    state.isIdle -> {
                        EmptyState(
                            message = strings.emptySearchPrompt,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    state.results.isEmpty() -> {
                        EmptyState(
                            message = strings.emptyForQuery(state.query),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // The list may be missing people the query never
                            // read. Say so rather than let it look complete.
                            if (state.reachedLimit) {
                                item(key = "reached-limit") {
                                    Text(
                                        text = strings.reachedLimit,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            items(
                                items = state.results,
                                key = { it.userId },
                            ) { player ->
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
}
