package com.walcker.games.features.ui.home.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.ui.home.map.model.MapPin
import com.walcker.match.cedar.components.LocalBottomBarInset
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.UIKit.UIColor
import platform.darwin.NSObject
import kotlin.math.pow

@OptIn(ExperimentalForeignApi::class)
private class MatchAnnotation(
    val matchId: String,
    val status: MatchStatus,
    private val lat: Double,
    private val lng: Double,
    private val annotationTitle: String,
    private val annotationSubtitle: String,
) : NSObject(),
    MKAnnotationProtocol {
    override fun coordinate(): CValue<CLLocationCoordinate2D> = CLLocationCoordinate2DMake(lat, lng)

    override fun title(): String = annotationTitle

    override fun subtitle(): String = annotationSubtitle
}

@OptIn(ExperimentalForeignApi::class)
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
    val currentOnPinClick = rememberUpdatedState(onPinClick)
    val mapView = remember { MKMapView() }

    val delegate =
        remember {
            object : NSObject(), MKMapViewDelegateProtocol {
                override fun mapView(
                    mapView: MKMapView,
                    didSelectAnnotationView: MKAnnotationView,
                ) {
                    val annotation = didSelectAnnotationView.annotation as? MatchAnnotation ?: return
                    currentOnPinClick.value(annotation.matchId)
                }

                override fun mapView(
                    mapView: MKMapView,
                    viewForAnnotation: MKAnnotationProtocol,
                ): MKAnnotationView? {
                    val annotation = viewForAnnotation as? MatchAnnotation ?: return null
                    val identifier = "match-pin"
                    val view =
                        mapView.dequeueReusableAnnotationViewWithIdentifier(identifier)
                            as? MKMarkerAnnotationView
                            ?: MKMarkerAnnotationView(annotation = annotation, reuseIdentifier = identifier)
                    view.annotation = annotation
                    view.canShowCallout = true
                    view.markerTintColor = annotation.status.markerColor()
                    return view
                }
            }
        }

    LaunchedEffect(Unit) {
        mapView.delegate = delegate
    }

    LaunchedEffect(hasLocationPermission) {
        mapView.showsUserLocation = hasLocationPermission
    }

    LaunchedEffect(camera.lat, camera.lng, camera.zoom) {
        val delta = 360.0 / 2.0.pow(camera.zoom.toDouble())
        val region =
            MKCoordinateRegionMake(
                CLLocationCoordinate2DMake(camera.lat, camera.lng),
                MKCoordinateSpanMake(delta, delta),
            )
        mapView.setRegion(region, animated = true)
    }

    LaunchedEffect(pins) {
        val existing = mapView.annotations.filterIsInstance<MatchAnnotation>()
        mapView.removeAnnotations(existing)
        val annotations =
            pins.map { pin ->
                MatchAnnotation(
                    matchId = pin.matchId,
                    status = pin.status,
                    lat = pin.lat,
                    lng = pin.lng,
                    annotationTitle = pin.title,
                    annotationSubtitle = pin.snippet,
                )
            }
        mapView.addAnnotations(annotations)
    }

    Box(modifier = modifier) {
        UIKitView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true),
        )

        if (nearbyCount > 0) {
            Button(
                onClick = onNearbyTap,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = LocalBottomBarInset.current + 16.dp),
            ) {
                Text("$nearbyCount próximas")
            }
        }
    }
}

private fun MatchStatus.markerColor(): UIColor =
    when (this) {
        MatchStatus.OPEN -> UIColor(red = 0.204, green = 0.780, blue = 0.349, alpha = 1.0)
        MatchStatus.FULL -> UIColor(red = 1.0, green = 0.584, blue = 0.0, alpha = 1.0)
        MatchStatus.CANCELLED -> UIColor(red = 1.0, green = 0.231, blue = 0.188, alpha = 1.0)
        MatchStatus.FINISHED -> UIColor(red = 0.686, green = 0.322, blue = 0.871, alpha = 1.0)
    }
