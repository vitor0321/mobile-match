package com.walcker.games.features.ui.create.locationPicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun LocationPickerMap(
    initialLat: Double,
    initialLng: Double,
    focusRequest: PickedLocation?,
    onLocationSettled: (PickedLocation) -> Unit,
    modifier: Modifier,
) {
    val currentOnLocationSettled = rememberUpdatedState(onLocationSettled)
    val mapView = remember { MKMapView() }

    val delegate =
        remember {
            object : NSObject(), MKMapViewDelegateProtocol {
                override fun mapView(
                    mapView: MKMapView,
                    regionDidChangeAnimated: Boolean,
                ) {
                    val center =
                        mapView.centerCoordinate.useContents {
                            PickedLocation(lat = latitude, lng = longitude)
                        }
                    currentOnLocationSettled.value(center)
                }
            }
        }

    LaunchedEffect(Unit) {
        mapView.delegate = delegate
        val region =
            MKCoordinateRegionMake(
                CLLocationCoordinate2DMake(initialLat, initialLng),
                MKCoordinateSpanMake(0.01, 0.01),
            )
        mapView.setRegion(region, animated = false)
        onLocationSettled(PickedLocation(lat = initialLat, lng = initialLng))
    }

    LaunchedEffect(focusRequest) {
        focusRequest?.let { location ->
            val region =
                MKCoordinateRegionMake(
                    CLLocationCoordinate2DMake(location.lat, location.lng),
                    MKCoordinateSpanMake(0.01, 0.01),
                )
            mapView.setRegion(region, animated = true)
        }
    }

    Box(modifier = modifier) {
        UIKitView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true),
        )
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .size(40.dp),
        )
    }
}
