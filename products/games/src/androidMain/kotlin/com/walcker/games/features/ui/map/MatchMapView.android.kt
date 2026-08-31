package com.walcker.games.features.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.walcker.games.features.domain.model.MatchStatus

@Composable
internal actual fun MatchMapView(
    pins: List<MapPin>,
    camera: MapCamera,
    onPinClick: (String) -> Unit,
    onNearbyTap: () -> Unit,
    nearbyCount: Int,
    hasLocationPermission: Boolean,
    modifier: Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(camera.lat, camera.lng),
            camera.zoom,
        )
    }

    LaunchedEffect(camera.lat, camera.lng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(camera.lat, camera.lng),
            camera.zoom,
        )
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = hasLocationPermission,
            ),
        ) {
            pins.forEach { pin ->
                Marker(
                    state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                    title = pin.title,
                    snippet = pin.snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(pin.status.markerHue()),
                    onClick = {
                        onPinClick(pin.matchId)
                        false
                    },
                )
            }
        }

        if (nearbyCount > 0) {
            Button(
                onClick = onNearbyTap,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Text("$nearbyCount próximas")
            }
        }
    }
}

private fun MatchStatus.markerHue(): Float = when (this) {
    MatchStatus.OPEN -> BitmapDescriptorFactory.HUE_GREEN
    MatchStatus.FULL -> BitmapDescriptorFactory.HUE_ORANGE
    MatchStatus.CANCELLED -> BitmapDescriptorFactory.HUE_RED
    MatchStatus.FINISHED -> BitmapDescriptorFactory.HUE_VIOLET
}
