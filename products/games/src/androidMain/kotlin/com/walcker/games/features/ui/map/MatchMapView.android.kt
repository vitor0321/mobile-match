package com.walcker.games.features.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.walcker.games.features.domain.model.MatchStatus
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Android implementation of [MatchMapView] using Google Maps via maps-compose.
 *
 * Marker hue encodes match status:
 * - OPEN  → green
 * - FULL  → orange
 * - other → red (cancelled/finished)
 */
@Composable
internal actual fun MatchMapView(
    pins: List<MapPin>,
    camera: MapCamera,
    onPinClick: (String) -> Unit,
    modifier: Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(camera.lat, camera.lng),
            camera.zoom,
        )
    }

    // Re-center when the camera target changes (e.g. after location resolves).
    LaunchedEffect(camera.lat, camera.lng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(camera.lat, camera.lng),
            camera.zoom,
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
    ) {
        pins.forEach { pin ->
            Marker(
                state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                title = pin.title,
                snippet = pin.snippet,
                icon = BitmapDescriptorFactory.defaultMarker(pin.status.markerHue()),
                onClick = {
                    onPinClick(pin.matchId)
                    false // allow default info-window behavior too
                },
            )
        }
    }
}

private fun MatchStatus.markerHue(): Float = when (this) {
    MatchStatus.OPEN -> BitmapDescriptorFactory.HUE_GREEN
    MatchStatus.FULL -> BitmapDescriptorFactory.HUE_ORANGE
    MatchStatus.CANCELLED -> BitmapDescriptorFactory.HUE_RED
    MatchStatus.FINISHED -> BitmapDescriptorFactory.HUE_VIOLET
}
