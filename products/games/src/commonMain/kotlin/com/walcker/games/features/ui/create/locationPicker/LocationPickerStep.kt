package com.walcker.games.features.ui.create.locationPicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.ui.create.component.LocationSummary
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSearchField
import com.walcker.match.cedar.tokens.CedarTokens
import org.koin.core.parameter.parametersOf

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
    onAddressQueryChanged: (String) -> Unit = {},
    onAddressSearchSubmit: () -> Unit = {},
    onConfirm: (lat: Double, lng: Double) -> Unit = { _, _ -> },
    mapBody: @Composable (Modifier) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = CedarTokens.colors.canvas,
        topBar = {
            CedarTopBar(
                title = strings.locationLabel,
                subtitle = strings.pickLocationHint,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CedarTokens.spacing.lg, vertical = CedarTokens.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                CedarSearchField(
                    value = state.addressQuery,
                    onValueChange = onAddressQueryChanged,
                    placeholder = strings.searchAddressPlaceholder,
                    onSearch = onAddressSearchSubmit,
                    enabled = !state.isSearching,
                )
                if (state.searchError) {
                    Text(
                        text = strings.addressNotFound,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            mapBody(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

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
