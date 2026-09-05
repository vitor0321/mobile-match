package com.walcker.games.features.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.ui.create.component.DateTimeSection
import com.walcker.games.features.ui.create.component.DurationPicker
import com.walcker.games.features.ui.create.component.FormTextField
import com.walcker.games.features.ui.create.component.PlayersSlider
import com.walcker.games.features.ui.create.component.RecurrencePicker
import com.walcker.games.features.ui.create.component.SportPicker
import com.walcker.games.features.ui.create.locationPicker.LocationPickerStep
import com.walcker.games.features.ui.shared.common.LoginRequiredBottomSheet
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarFilterRow
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.LocalBottomBarInset
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.LoginCoordinator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

internal class CreateMatchStep(
    private val matchId: String? = null,
) : Screen {
    override val key: String get() = "create-match-${matchId ?: "new"}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel =
            koinScreenModel<CreateMatchStepModel>(
                parameters = { parametersOf(matchId) },
            )
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
                    CreateMatchEffect.MatchUpdated -> navigator.pop()
                    is CreateMatchEffect.RequireLogin -> showLoginSheet = true
                }
            }
        }

        CreateMatchContent(
            state = state,
            onEvent = stepModel::onEvent,
            strings = strings,
            onBack = { navigator.pop() },
            onLocationPick = { lat, lng ->
                navigator.push(
                    LocationPickerStep(
                        initialLat = lat,
                        initialLng = lng,
                        onConfirm = { newLat, newLng ->
                            stepModel.onEvent(
                                CreateMatchEvents.LocationSelected(newLat, newLng),
                            )
                        },
                    ),
                )
            },
            snackbarHostState = snackbarHostState,
        )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateMatchContent(
    state: CreateMatchState,
    onEvent: (CreateMatchEvents) -> Unit,
    strings: CreateMatchStrings,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onLocationPick: (lat: Double, lng: Double) -> Unit = { _, _ -> },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val enabled = !state.isSubmitting

    if (state.isLoading) {
        Scaffold(modifier = modifier, containerColor = CedarTokens.colors.canvas) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CedarLoading(contentDescription = strings.editTitle)
            }
        }
        return
    }

    Scaffold(
        modifier = modifier,
        containerColor = CedarTokens.colors.canvas,
        topBar = {
            CedarTopBar(
                title = if (state.isEditMode) strings.editTitle else strings.title,
                subtitle = if (state.isEditMode) strings.editSubtitle else strings.subtitle,
                onBack = if (state.isEditMode) onBack else null,
                backContentDescription = strings.backContentDescription,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding)
                    .padding(padding)
                    .imePadding(),
            contentPadding =
                PaddingValues(
                    top = CedarTokens.spacing.md,
                    bottom = CedarTokens.spacing.md + LocalBottomBarInset.current,
                ),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
        ) {
            item(key = "match-name") {
                FormTextField(
                    value = state.venueName,
                    onValueChange = {
                        onEvent(CreateMatchEvents.VenueNameChanged(it))
                    },
                    label = strings.matchNameLabel,
                    placeholder = strings.matchNamePlaceholder,
                    error = state.venueNameError,
                    enabled = enabled,
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "sport") {
                Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs)) {
                    Text(
                        text = strings.sportLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                    )
                    SportPicker(
                        selected = state.selectedSport,
                        enabled = enabled,
                        mySports = state.mySports,
                        onSelect = { onEvent(CreateMatchEvents.SportSelected(it)) },
                    )
                }
            }

            item(key = "location") {
                val summary =
                    listOf(state.address, state.neighborhood, state.city)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                CedarFilterRow(
                    label = strings.locationLabel,
                    value = summary.takeIf { it.isNotBlank() },
                    placeholder =
                        if (state.isResolvingLocation) {
                            strings.resolvingLocation
                        } else {
                            strings.chooseLocationLabel
                        },
                    onClick = {
                        val lat = state.lat
                        val lng = state.lng
                        if (lat != null && lng != null) {
                            onLocationPick(lat, lng)
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "section-when") {
                CedarSectionHeader(
                    title = strings.sectionWhen,
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "date-time") {
                DateTimeSection(
                    selectedDateMillis = state.selectedDate,
                    selectedTime = state.selectedTime,
                    dateError = state.dateError,
                    timeError = state.timeError,
                    strings = strings,
                    enabled = enabled,
                    onDateSelected = { onEvent(CreateMatchEvents.DateSelected(it)) },
                    onTimeSelected = { hour, minute ->
                        onEvent(CreateMatchEvents.TimeSelected(hour, minute))
                    },
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "duration") {
                DurationPicker(
                    selectedDurationMin = state.durationMin,
                    strings = strings,
                    enabled = enabled,
                    onDurationSelected = {
                        onEvent(CreateMatchEvents.DurationSelected(it))
                    },
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "section-details") {
                CedarSectionHeader(
                    title = strings.sectionDetails,
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "players") {
                PlayersSlider(
                    totalPlayers = state.totalPlayers,
                    strings = strings,
                    enabled = enabled,
                    onPlayersChanged = {
                        onEvent(CreateMatchEvents.PlayersChanged(it))
                    },
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "recurrence") {
                Column(
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                ) {
                    RecurrencePicker(
                        selected = state.recurrence,
                        strings = strings,
                        enabled = enabled,
                        onRecurrenceSelected = {
                            onEvent(CreateMatchEvents.RecurrenceSelected(it))
                        },
                    )
                    if (state.recurrence != RecurrenceOption.NONE) {
                        Text(
                            text = strings.recurrenceAutoCreateNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "price") {
                FormTextField(
                    value = state.pricePerPlayer,
                    onValueChange = { onEvent(CreateMatchEvents.PriceChanged(it)) },
                    label = strings.priceLabel,
                    placeholder = strings.pricePlaceholder,
                    error = state.priceError,
                    helper = strings.priceHelper,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
                )
            }

            item(key = "submit") {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CedarTokens.spacing.lg, vertical = CedarTokens.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CedarPrimaryButton(
                        text = if (state.isEditMode) strings.saveChanges else strings.submit,
                        onClick = { onEvent(CreateMatchEvents.Submit) },
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
}
