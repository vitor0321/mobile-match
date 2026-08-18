package com.walcker.games.features.ui.search

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
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
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.MatchCard

/**
 * Search screen — filters the local cache by venue, neighborhood,
 * city or sport label. Search runs entirely client-side; the cache
 * is updated whenever the home screen refreshes.
 */
internal class SearchStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<SearchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val gamesStrings = rememberGamesStrings().strings
        val strings = gamesStrings.search
        val cardStrings = gamesStrings.gameList

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is SearchEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        if (state.showFiltersPanel) {
            ModalBottomSheet(
                onDismissRequest = { stepModel.onEvent(SearchEvents.ToggleFiltersPanel) },
            ) {
                SearchFiltersPanel(
                    state = state,
                    onDateRangeChanged = { start, end ->
                        stepModel.onEvent(SearchEvents.DateRangeChanged(start, end))
                    },
                    onSportFilterChanged = { sports ->
                        stepModel.onEvent(SearchEvents.SportFilterChanged(sports))
                    },
                    onPriceRangeChanged = { min, max ->
                        stepModel.onEvent(SearchEvents.PriceRangeChanged(min, max))
                    },
                    onResetFilters = { stepModel.onEvent(SearchEvents.ResetFilters) },
                    onDismiss = { stepModel.onEvent(SearchEvents.ToggleFiltersPanel) },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { stepModel.onEvent(SearchEvents.QueryChanged(it)) },
                        modifier = Modifier
                            .weight(1f),
                        placeholder = { Text(strings.placeholder) },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = { stepModel.onEvent(SearchEvents.ToggleFiltersPanel) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Filtros",
                        )
                    }
                }

                if (state.results.isEmpty() && state.query.isNotBlank()) {
                    EmptyState(
                        message = strings.emptyForQuery(state.query),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.results, key = { it.id }) { game ->
                            val playersLabel = cardStrings.playersAndSlots(
                                game.confirmedPlayers,
                                game.totalPlayers,
                                game.openSlots,
                                if (game.openSlots == 1) "vaga" else "vagas",
                            )
                            MatchCard(
                                sportLabel = game.sport.label,
                                venueName = game.venueName,
                                neighborhood = game.neighborhood,
                                startsAtSeconds = game.startsAtSeconds,
                                confirmedPlayers = game.confirmedPlayers,
                                totalPlayers = game.totalPlayers,
                                openSlots = game.openSlots,
                                pricePerPlayer = game.pricePerPlayer,
                                joinButtonLabel = cardStrings.joinButton,
                                playersAndSlotsLabel = playersLabel,
                                perPlayerLabel = game.pricePerPlayer?.let { cardStrings.perPlayer(it) },
                                onJoinClick = { stepModel.onEvent(SearchEvents.JoinGame(game.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFiltersPanel(
    state: SearchState,
    onDateRangeChanged: (startMs: Long?, endMs: Long?) -> Unit,
    onSportFilterChanged: (sports: Set<com.walcker.games.features.domain.model.Sport>) -> Unit,
    onPriceRangeChanged: (minPrice: Float?, maxPrice: Float?) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Filtros",
            style = MaterialTheme.typography.headlineSmall,
        )

        // Date range section
        Text(
            text = "Data",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Selecione um intervalo de datas (em breve)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Sport filter section
        Text(
            text = "Esporte",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Selecione os esportes desejados (em breve)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Price range section
        Text(
            text = "Preço",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Selecione uma faixa de preço (em breve)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onResetFilters,
                modifier = Modifier.weight(1f),
            ) {
                Text("Limpar")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Aplicar")
            }
        }

        // Spacer for bottom sheet navigation
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
    }
}
