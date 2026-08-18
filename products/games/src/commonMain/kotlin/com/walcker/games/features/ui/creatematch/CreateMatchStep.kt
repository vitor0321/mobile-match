package com.walcker.games.features.ui.creatematch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

    @OptIn(ExperimentalMaterial3Api::class)
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
                        // The TabCoordinator in MatchScaffold is what actually
                        // switches tabs; here we just acknowledge to the user.
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
                    //
                    // Column, not LazyColumn: a lazy list inside a LazyColumn
                    // item is measured with infinite height and crashes at
                    // runtime. Sport is a small fixed enum anyway — there is
                    // nothing to virtualize.
                    item {
                        Text("Esporte", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Sport.entries.forEach { sport ->
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

                    // Date + Time picker section
                    item {
                        DateTimePickerSection(
                            selectedDateMillis = state.selectedDate,
                            selectedTime = state.selectedTime,
                            onDateSelected = { stepModel.onEvent(CreateMatchEvents.DateSelected(it)) },
                            onTimeSelected = { hour, minute ->
                                stepModel.onEvent(CreateMatchEvents.TimeSelected(hour, minute))
                            },
                            dateError = state.dateError,
                            timeError = state.timeError,
                        )
                    }

                    // Duration dropdown (60 / 90 / 120)
                    item {
                        DurationDropdown(
                            selectedDurationMin = state.durationMin,
                            onDurationSelected = { stepModel.onEvent(CreateMatchEvents.DurationSelected(it)) },
                        )
                    }

                    // Players count (slider)
                    item {
                        PlayersSlider(
                            totalPlayers = state.totalPlayers,
                            onPlayersChanged = { stepModel.onEvent(CreateMatchEvents.PlayersChanged(it)) },
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

/**
 * Combined date + time picker row. Two outlined buttons open Material3
 * dialogs; the chosen date and time are surfaced as plain text inside each
 * button so the user can always see what is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerSection(
    selectedDateMillis: Long?,
    selectedTime: Pair<Int, Int>?,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    dateError: String?,
    timeError: String?,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Data e horário", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = selectedDateMillis?.let { formatDate(it) } ?: "Selecionar data",
                )
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = selectedTime?.let { formatTime(it.first, it.second) } ?: "Selecionar hora",
                )
            }
        }
        if (dateError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(dateError, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
        if (timeError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(timeError, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialHour = selectedTime?.first ?: 19
        val initialMinute = selectedTime?.second ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationDropdown(
    selectedDurationMin: Int,
    onDurationSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Duração", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("$selectedDurationMin minutos")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CreateMatchState.AVAILABLE_DURATIONS.forEach { duration ->
                DropdownMenuItem(
                    text = { Text("$duration minutos") },
                    onClick = {
                        onDurationSelected(duration)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PlayersSlider(
    totalPlayers: Int,
    onPlayersChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Total de jogadores: $totalPlayers",
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Round to steps of 1; the state already constrains the range to
        // [MIN_PLAYERS, MAX_PLAYERS] via CreateMatchState.isFormValid.
        Slider(
            value = totalPlayers.toFloat(),
            onValueChange = { onPlayersChanged(it.toInt()) },
            valueRange = CreateMatchState.MIN_PLAYERS.toFloat()..CreateMatchState.MAX_PLAYERS.toFloat(),
            steps = (CreateMatchState.MAX_PLAYERS - CreateMatchState.MIN_PLAYERS - 1),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatDate(millis: Long): String {
    val local = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.dayOfMonth.toString().padStart(2, '0')}/" +
            "${local.monthNumber.toString().padStart(2, '0')}/" +
            "${local.year}"
}

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
