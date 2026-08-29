package com.walcker.games.features.ui.creatematch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.features.ui.common.LoginRequiredBottomSheet
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarFilterRow
import com.walcker.match.cedar.components.CedarFilterSection
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.LoginCoordinator
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private const val DEFAULT_HOUR = 19

internal class CreateMatchStep : Screen {

    override val key: String get() = "create-match"

    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<CreateMatchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val strings = rememberGamesStrings().strings.createMatch
        val loginCoordinator = koinInject<LoginCoordinator>()
        val loginRequired = rememberGamesStrings().strings.loginRequired
        var showLoginSheet by remember { mutableStateOf(false) }

        LaunchedEffect(stepModel) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is CreateMatchEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)
                    is CreateMatchEffect.NavigateToMyMatches ->
                        snackbarHostState.showSnackbar(strings.success)
                    is CreateMatchEffect.RequireLogin -> showLoginSheet = true
                }
            }
        }

        val enabled = !state.isSubmitting

        Scaffold(
            containerColor = CedarTokens.colors.canvas,
            topBar = {
                CedarTopBar(
                    title = strings.title,
                    subtitle = strings.subtitle,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            ) {
                item(key = "section-venue") {
                    CedarSectionHeader(title = strings.sectionVenue)
                }

                item(key = "venue-name") {
                    FormTextField(
                        value = state.venueName,
                        onValueChange = {
                            stepModel.onEvent(CreateMatchEvents.VenueNameChanged(it))
                        },
                        label = strings.venueNameLabel,
                        placeholder = strings.venueNamePlaceholder,
                        error = state.venueNameError,
                        enabled = enabled,
                    )
                }

                item(key = "sport") {
                    CedarFilterSection(label = strings.sportLabel) {
                        SportPicker(
                            selected = state.selectedSport,
                            enabled = enabled,
                            onSelect = { stepModel.onEvent(CreateMatchEvents.SportSelected(it)) },
                        )
                    }
                }

                item(key = "neighborhood") {
                    FormTextField(
                        value = state.neighborhood,
                        onValueChange = {
                            stepModel.onEvent(CreateMatchEvents.NeighborhoodChanged(it))
                        },
                        label = strings.neighborhoodLabel,
                        placeholder = strings.neighborhoodPlaceholder,
                        error = state.neighborhoodError,
                        enabled = enabled,
                    )
                }

                item(key = "city") {
                    FormTextField(
                        value = state.city,
                        onValueChange = { stepModel.onEvent(CreateMatchEvents.CityChanged(it)) },
                        label = strings.cityLabel,
                        placeholder = strings.cityPlaceholder,
                        error = state.cityError,
                        enabled = enabled,
                    )
                }

                item(key = "address") {
                    FormTextField(
                        value = state.address,
                        onValueChange = { stepModel.onEvent(CreateMatchEvents.AddressChanged(it)) },
                        label = strings.addressLabel,
                        placeholder = strings.addressPlaceholder,
                        error = state.addressError,
                        enabled = enabled,
                    )
                }

                item(key = "section-when") {
                    CedarSectionHeader(title = strings.sectionWhen)
                }

                item(key = "date-time") {
                    DateTimeSection(
                        selectedDateMillis = state.selectedDate,
                        selectedTime = state.selectedTime,
                        dateError = state.dateError,
                        timeError = state.timeError,
                        strings = strings,
                        enabled = enabled,
                        onDateSelected = { stepModel.onEvent(CreateMatchEvents.DateSelected(it)) },
                        onTimeSelected = { hour, minute ->
                            stepModel.onEvent(CreateMatchEvents.TimeSelected(hour, minute))
                        },
                    )
                }

                item(key = "duration") {
                    DurationPicker(
                        selectedDurationMin = state.durationMin,
                        strings = strings,
                        enabled = enabled,
                        onDurationSelected = {
                            stepModel.onEvent(CreateMatchEvents.DurationSelected(it))
                        },
                    )
                }

                item(key = "section-details") {
                    CedarSectionHeader(title = strings.sectionDetails)
                }

                item(key = "players") {
                    PlayersSlider(
                        totalPlayers = state.totalPlayers,
                        strings = strings,
                        enabled = enabled,
                        onPlayersChanged = {
                            stepModel.onEvent(CreateMatchEvents.PlayersChanged(it))
                        },
                    )
                }

                item(key = "price") {
                    FormTextField(
                        value = state.pricePerPlayer,
                        onValueChange = { stepModel.onEvent(CreateMatchEvents.PriceChanged(it)) },
                        label = strings.priceLabel,
                        placeholder = strings.pricePlaceholder,
                        error = state.priceError,
                        helper = strings.priceHelper,
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }

                item(key = "submit") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = CedarTokens.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CedarPrimaryButton(
                            text = strings.submit,
                            onClick = { stepModel.onEvent(CreateMatchEvents.Submit) },
                            enabled = state.isFormValid,
                            loading = state.isSubmitting,
                        )
                        if (!state.isFormValid) {
                            Text(
                                text = strings.validationError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        LoginRequiredBottomSheet(
            isVisible = showLoginSheet,
            strings = loginRequired,
            onConfirm = {
                loginCoordinator.requestLogin()
                showLoginSheet = false
            },
            onDismiss = { showLoginSheet = false },
        )
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
    ),
) {
    val supporting = error ?: helper

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        supportingText = supporting?.let { text -> { Text(text) } },
        isError = error != null,
        singleLine = true,
        shape = CedarTokens.radius.smShape,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SportPicker(
    selected: Sport?,
    enabled: Boolean,
    onSelect: (Sport) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Sport.entries.forEach { sport ->
            SportChip(
                label = sport.label,
                selected = selected == sport,
                onClick = { onSelect(sport) },
                enabled = enabled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeSection(
    selectedDateMillis: Long?,
    selectedTime: Pair<Int, Int>?,
    dateError: String?,
    timeError: String?,
    strings: CreateMatchStrings,
    enabled: Boolean,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            CedarFilterRow(
                label = strings.dateLabel,
                value = selectedDateMillis?.let(::formatDate),
                placeholder = strings.datePlaceholder,
                onClick = { showDatePicker = true },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            CedarFilterRow(
                label = strings.timeLabel,
                value = selectedTime?.let { formatTime(it.first, it.second) },
                placeholder = strings.timePlaceholder,
                onClick = { showTimePicker = true },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }

        val error = dateError ?: timeError
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateSelected)
                        showDatePicker = false
                    },
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.first ?: DEFAULT_HOUR,
            initialMinute = selectedTime?.second ?: 0,
            is24Hour = true,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = CedarTokens.radius.lgShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = CedarTokens.elevation.overlay,
            ) {
                Column(
                    modifier = Modifier.padding(CedarTokens.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(strings.cancel)
                        }
                        TextButton(
                            onClick = {
                                onTimeSelected(timePickerState.hour, timePickerState.minute)
                                showTimePicker = false
                            },
                        ) { Text(strings.confirm) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationPicker(
    selectedDurationMin: Int,
    strings: CreateMatchStrings,
    enabled: Boolean,
    onDurationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        CedarFilterRow(
            label = strings.durationLabel,
            value = strings.durationValue(selectedDurationMin),
            placeholder = strings.durationLabel,
            onClick = { expanded = true },
            enabled = enabled,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CreateMatchState.AVAILABLE_DURATIONS.forEach { duration ->
                DropdownMenuItem(
                    text = { Text(strings.durationValue(duration)) },
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
    strings: CreateMatchStrings,
    enabled: Boolean,
    onPlayersChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Text(
            text = strings.playersLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = strings.playersValue(totalPlayers),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = totalPlayers.toFloat(),
            onValueChange = { onPlayersChanged(it.toInt()) },
            valueRange = CreateMatchState.MIN_PLAYERS.toFloat()..
                CreateMatchState.MAX_PLAYERS.toFloat(),
            steps = CreateMatchState.MAX_PLAYERS - CreateMatchState.MIN_PLAYERS - 1,
            enabled = enabled,
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
