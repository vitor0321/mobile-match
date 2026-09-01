package com.walcker.games.features.ui.home.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.ui.home.map.model.MapPin

internal data class MapCamera(
    val lat: Double,
    val lng: Double,
    val zoom: Float = 13f,
)

@Composable
internal expect fun MatchMapView(
    pins: List<MapPin>,
    camera: MapCamera,
    onPinClick: (String) -> Unit,
    onNearbyTap: () -> Unit,
    nearbyCount: Int,
    hasLocationPermission: Boolean,
    modifier: Modifier,
)
