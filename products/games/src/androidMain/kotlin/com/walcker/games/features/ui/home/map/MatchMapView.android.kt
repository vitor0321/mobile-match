package com.walcker.games.features.ui.home.map

import android.R.attr.strokeWidth
import android.R.attr.textSize
import android.R.attr.typeface
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.system.Os.close
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
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
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.ui.home.map.model.MapPin
import com.walcker.games.features.ui.shared.map.rememberGoogleMapStyleOptions
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

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
    val context = LocalContext.current
    val cameraPositionState =
        rememberCameraPositionState {
            position =
                CameraPosition.fromLatLngZoom(
                    LatLng(camera.lat, camera.lng),
                    camera.zoom,
                )
        }
    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = rememberGoogleMapStyleOptions(isDarkTheme)

    LaunchedEffect(camera.lat, camera.lng) {
        cameraPositionState.position =
            CameraPosition.fromLatLngZoom(
                LatLng(camera.lat, camera.lng),
                camera.zoom,
            )
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapStyleOptions = mapStyleOptions,
                ),
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = hasLocationPermission,
                ),
        ) {
            pins.forEach { pin ->
                val markerIcon =
                    remember(pin.title, pin.status, isDarkTheme) {
                        createLabeledMarkerBitmap(
                            context = context,
                            title = pin.title,
                            tint = pin.status.markerColor(),
                            isDarkTheme = isDarkTheme,
                        )
                    }
                Marker(
                    state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                    title = pin.title,
                    snippet = pin.snippet,
                    icon = BitmapDescriptorFactory.fromBitmap(markerIcon.bitmap),
                    anchor = Offset(0.5f, markerIcon.anchorY),
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
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
            ) {
                Text("$nearbyCount próximas")
            }
        }
    }
}

private fun MatchStatus.markerColor(): Int =
    when (this) {
        MatchStatus.OPEN -> Color.rgb(52, 199, 89)
        MatchStatus.FULL -> Color.rgb(255, 149, 0)
        MatchStatus.CANCELLED -> Color.rgb(255, 59, 48)
        MatchStatus.FINISHED -> Color.rgb(175, 82, 222)
    }

private class LabeledMarkerBitmap(
    val bitmap: Bitmap,
    val anchorY: Float,
)

private fun ellipsize(
    text: String,
    paint: Paint,
    maxWidth: Float,
): String {
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
        end--
    }
    return text.substring(0, end) + ellipsis
}

private fun createLabeledMarkerBitmap(
    context: Context,
    title: String,
    tint: Int,
    isDarkTheme: Boolean,
): LabeledMarkerBitmap {
    val density = context.resources.displayMetrics.density

    fun dp(value: Float) = value * density

    val pinRadius = dp(11f)
    val pinTipLength = pinRadius * 1.8f
    val pinStrokeWidth = dp(1.5f)
    val gap = dp(4f)
    val labelPaddingH = dp(10f)
    val labelPaddingV = dp(6f)
    val labelCornerRadius = dp(10f)
    val maxLabelWidth = dp(160f)
    val shadowBleed = dp(4f)

    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) Color.WHITE else Color.BLACK
            textSize = dp(12f)
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

    val displayTitle = ellipsize(title, textPaint, maxLabelWidth)
    val textWidth = textPaint.measureText(displayTitle)
    val fontMetrics = textPaint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent

    val labelWidth = textWidth + labelPaddingH * 2
    val labelHeight = textHeight + labelPaddingV * 2
    val pinWidth = pinRadius * 2 + pinStrokeWidth
    val pinHeight = pinRadius + pinTipLength + pinStrokeWidth

    val contentWidth = maxOf(labelWidth, pinWidth)
    val contentHeight = labelHeight + gap + pinHeight

    val bitmapWidth = (contentWidth + shadowBleed * 2).toInt().coerceAtLeast(1)
    val bitmapHeight = (contentHeight + shadowBleed * 2).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val labelLeft = (bitmapWidth - labelWidth) / 2f
    val labelTop = shadowBleed
    val labelRect = RectF(labelLeft, labelTop, labelLeft + labelWidth, labelTop + labelHeight)

    val labelBackgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) Color.rgb(28, 28, 30) else Color.WHITE
            setShadowLayer(shadowBleed, 0f, dp(1f), Color.argb(70, 0, 0, 0))
        }
    canvas.drawRoundRect(labelRect, labelCornerRadius, labelCornerRadius, labelBackgroundPaint)

    val textY = labelRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(displayTitle, labelRect.centerX(), textY, textPaint)

    val pinCenterX = bitmapWidth / 2f
    val pinCenterY = labelRect.bottom + gap + pinRadius + pinStrokeWidth / 2f
    val tipY = pinCenterY + pinTipLength

    val tangentAngleDeg = Math.toDegrees(acos((pinRadius / pinTipLength).toDouble())).toFloat()
    val leftTangentAngle = 90f + tangentAngleDeg
    val rightTangentAngle = 90f - tangentAngleDeg
    val arcSweep = 360f - 2 * tangentAngleDeg

    fun pointOnPinCircle(angleDegrees: Float): Pair<Float, Float> {
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        return (pinCenterX + pinRadius * cos(angleRad).toFloat()) to
            (pinCenterY + pinRadius * sin(angleRad).toFloat())
    }

    val (leftTangentX, leftTangentY) = pointOnPinCircle(leftTangentAngle)
    val pinPath =
        Path().apply {
            moveTo(pinCenterX, tipY)
            lineTo(leftTangentX, leftTangentY)
            arcTo(
                RectF(
                    pinCenterX - pinRadius,
                    pinCenterY - pinRadius,
                    pinCenterX + pinRadius,
                    pinCenterY + pinRadius,
                ),
                leftTangentAngle,
                arcSweep,
                false,
            )
            lineTo(pinCenterX, tipY)
            close()
        }

    val pinFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
    canvas.drawPath(pinPath, pinFillPaint)

    val pinStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = pinStrokeWidth
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
    canvas.drawPath(pinPath, pinStrokePaint)

    val anchorY = tipY / bitmapHeight
    return LabeledMarkerBitmap(bitmap = bitmap, anchorY = anchorY)
}
