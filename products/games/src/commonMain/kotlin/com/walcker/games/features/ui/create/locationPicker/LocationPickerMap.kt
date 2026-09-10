package com.walcker.games.features.ui.create.locationPicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal data class PickedLocation(
    val lat: Double,
    val lng: Double,
)

@Composable
internal expect fun LocationPickerMap(
    initialLat: Double,
    initialLng: Double,
    focusRequest: PickedLocation?,
    onLocationSettled: (PickedLocation) -> Unit,
    modifier: Modifier,
)
