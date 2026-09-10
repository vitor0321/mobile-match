package com.walcker.games.features.ui.home.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.ui.home.map.model.MapPin
import kotlin.math.pow

private const val KM_PER_DEGREE_LATITUDE = 110.574

internal data class MapCamera(
    val lat: Double,
    val lng: Double,
    val zoom: Float = 13f,
) {
    val approximateRadiusKm: Double
        get() {
            val spanDegrees = 360.0 / 2.0.pow(zoom.toDouble())
            return (spanDegrees / 2.0) * KM_PER_DEGREE_LATITUDE
        }
}

@Composable
internal expect fun MatchMapView(
    pins: List<MapPin>,
    camera: MapCamera,
    onPinClick: (String) -> Unit,
    onNearbyTap: () -> Unit,
    nearbyCount: Int,
    hasLocationPermission: Boolean,
    modifier: Modifier,
    onCameraIdle: (MapCamera) -> Unit,
)
