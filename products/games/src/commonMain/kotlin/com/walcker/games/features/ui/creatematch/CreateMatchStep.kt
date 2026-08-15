package com.walcker.games.features.ui.creatematch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar

/**
 * Create Match screen — form to create a new match.
 *
 * Form sections:
 * - Venue (name, neighborhood, city, address)
 * - Sport selection
 * - DateTime (date + time picker)
 * - Players count (slider)
 * - Price (optional)
 *
 * Simplified for ETAPA1. Date/time pickers to be implemented in ETAPA2.
 */
internal class CreateMatchStep : Screen {

    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<CreateMatchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val strings = rememberGamesStrings().strings.gameList

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is CreateMatchEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)
                    is CreateMatchEffect.NavigateToMyMatches -> {
                        // TODO: Navigate to MyMatches tab and highlight the new match
                        snackbarHostState.showSnackbar("Match criada: ${effect.matchId}")
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                CedarTopBar(
                    title = "Criar Partida",
                    subtitle = "Preencha os detalhes da partida",
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            if (state.isSubmitting) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Text("Seu espaço", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                    }

                    // Venue name
                    item {
                        OutlinedTextField(
                            value = state.venueName,
                            onValueChange = { stepModel.onEvent(CreateMatchEvents.VenueNameChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nome do local") },
                            placeholder = { Text("Ex: Quadra do Parque") },
                            singleLine = true,
                            isError = state.venueNameError != null,
                        )
                        if (state.venueNameError != null) {
                            Text(state.venueNameError!!, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Sport selection
                    item {
                        Text("Esporte", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items = Sport.values().toList()) { sport ->
                                FilterChip(
                                    selected = state.selectedSport == sport,
                                    onClick = { stepModel.onEvent(CreateMatchEvents.SportSelected(sport)) },
                                    label = { Text(sport.label) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    // Neighborhood
                    item {
                        OutlinedTextField(
                            value = state.neighborhood,
                            onValueChange = { stepModel.onEvent(CreateMatchEvents.NeighborhoodChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Bairro") },
                            placeholder = { Text("Ex: Centro") },
                            singleLine = true,
                        )
                    }

                    // City
                    item {
                        OutlinedTextField(
                            value = state.city,
                            onValueChange = { stepModel.onEvent(CreateMatchEvents.CityChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Cidade") },
                            placeholder = { Text("Ex: São Paulo") },
                            singleLine = true,
                        )
                    }

                    // Address
                    item {
                        OutlinedTextField(
                            value = state.address,
                            onValueChange = { stepModel.onEvent(CreateMatchEvents.AddressChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Endereço") },
                            placeholder = { Text("Ex: Rua A, 123") },
                            singleLine = true,
                        )
                    }

                    // TODO: Add DateTimePicker, PlayersSlider, PriceInput

                    // Players count (placeholder)
                    item {
                        Text("Vagas disponíveis: ${state.totalPlayers - 1}")
                        OutlinedTextField(
                            value = state.totalPlayers.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { stepModel.onEvent(CreateMatchEvents.PlayersChanged(it)) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Total de jogadores") },
                            singleLine = true,
                        )
                    }

                    // Price (optional)
                    item {
                        OutlinedTextField(
                            value = state.pricePerPlayer,
                            onValueChange = { stepModel.onEvent(CreateMatchEvents.PriceChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Preço por jogador (opcional)") },
                            placeholder = { Text("Ex: R$ 50") },
                            singleLine = true,
                        )
                    }

                    // Submit button
                    item {
                        Button(
                            onClick = { stepModel.onEvent(CreateMatchEvents.Submit) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            enabled = state.isFormValid && !state.isSubmitting,
                        ) {
                            Text("Criar e Publicar")
                        }
                    }
                }
            }
        }
    }
}
