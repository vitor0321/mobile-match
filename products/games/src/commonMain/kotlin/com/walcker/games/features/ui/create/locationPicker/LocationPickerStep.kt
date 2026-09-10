package com.walcker.games.features.ui.create.locationPicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.ui.create.component.LocationSummary
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSearchField
import com.walcker.match.cedar.components.LocalBottomBarInset
import com.walcker.match.cedar.tokens.CedarTokens
import org.koin.core.parameter.parametersOf

private val ControlButtonSize = 48.dp

internal class LocationPickerStep(
    private val initialLat: Double,
    private val initialLng: Double,
    private val onConfirm: (Double, Double) -> Unit,
) : Screen {
    override val key: String get() = "create-match-location-picker"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel =
            koinScreenModel<LocationPickerStepModel>(
                parameters = { parametersOf(initialLat, initialLng) },
            )
        val state by stepModel.state.collectAsState()
        val strings = rememberGamesStrings().strings.createMatch

        LocationPickerContent(
            state = state,
            strings = strings,
            onBack = { navigator.pop() },
            onAddressQueryChanged = stepModel::onAddressQueryChanged,
            onAddressSearchSubmit = stepModel::onAddressSearchSubmit,
            onConfirm = { lat, lng ->
                onConfirm(lat, lng)
                navigator.pop()
            },
            mapBody = { bodyModifier ->
                LocationPickerMap(
                    initialLat = state.lat,
                    initialLng = state.lng,
                    focusRequest = state.focusRequest,
                    onLocationSettled = { picked ->
                        stepModel.onLocationChanged(picked.lat, picked.lng)
                    },
                    modifier = bodyModifier,
                )
            },
        )
    }
}

@Composable
internal fun LocationPickerContent(
    state: LocationPickerState,
    strings: CreateMatchStrings,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAddressQueryChanged: (String) -> Unit = {},
    onAddressSearchSubmit: () -> Unit = {},
    onConfirm: (lat: Double, lng: Double) -> Unit = { _, _ -> },
    mapBody: @Composable (Modifier) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        mapBody(Modifier.fillMaxSize())

        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = CedarTokens.spacing.md, vertical = CedarTokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = CedarTokens.elevation.overlay,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(ControlButtonSize),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backContentDescription,
                        )
                    }
                }
                CedarSearchField(
                    value = state.addressQuery,
                    onValueChange = onAddressQueryChanged,
                    placeholder = strings.searchAddressPlaceholder,
                    onSearch = onAddressSearchSubmit,
                    enabled = !state.isSearching,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.searchError) {
                Card(
                    shape = CedarTokens.radius.mdShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = strings.addressNotFound,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier =
                            Modifier.padding(
                                horizontal = CedarTokens.spacing.md,
                                vertical = CedarTokens.spacing.xs,
                            ),
                    )
                }
            }
        }

        Card(
            shape = CedarTokens.radius.mdShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.overlay),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = CedarTokens.spacing.md,
                        end = CedarTokens.spacing.md,
                        top = CedarTokens.spacing.md,
                        bottom = CedarTokens.spacing.md + LocalBottomBarInset.current,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(CedarTokens.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                LocationSummary(
                    strings = strings,
                    address = state.address,
                    neighborhood = state.neighborhood,
                    city = state.city,
                    isResolvingLocation = state.isResolvingLocation,
                )
                CedarPrimaryButton(
                    text = strings.confirmLocationLabel,
                    onClick = { onConfirm(state.lat, state.lng) },
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
