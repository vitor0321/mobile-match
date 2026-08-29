package com.walcker.match.cedar.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

public enum class CedarBrand { Blue, Green }

@Immutable
public data class CedarColors(
    val canvas: Color,
    val surfaceSubtle: Color,
    val available: Color,
    val onAvailable: Color,
    val availableText: Color,
    val availableContainer: Color,
    val mapBase: Color,
    val mapLine: Color,
    val imagePlaceholder: Color,
    val overlayScrim: Color,
    val splashBackground: Color,
)

internal val CedarLightColors: CedarColors = CedarColors(
    canvas = CedarPalette.Canvas,
    surfaceSubtle = CedarPalette.SurfaceSubtle,
    available = CedarPalette.Green500,
    onAvailable = CedarPalette.Ink900,
    availableText = CedarPalette.Green700,
    availableContainer = CedarPalette.Green100,
    mapBase = CedarPalette.MapBase,
    mapLine = CedarPalette.MapLine,
    imagePlaceholder = CedarPalette.ImagePlaceholder,
    overlayScrim = Color(0x66091729),
    splashBackground = CedarPalette.SplashBackground,
)

internal val CedarDarkColors: CedarColors = CedarColors(
    canvas = CedarPalette.CanvasDark,
    surfaceSubtle = CedarPalette.SurfaceSubtleDark,
    available = CedarPalette.Green400,
    onAvailable = CedarPalette.Ink900,
    availableText = CedarPalette.Green400,
    availableContainer = CedarPalette.Green900,
    mapBase = CedarPalette.MapBaseDark,
    mapLine = CedarPalette.MapLineDark,
    imagePlaceholder = CedarPalette.ImagePlaceholderDark,
    overlayScrim = Color(0x990B1220),
    splashBackground = CedarPalette.SplashBackground,
)

internal fun cedarLightColorScheme(brand: CedarBrand): ColorScheme {
    val primary = when (brand) {
        CedarBrand.Blue -> CedarPalette.Blue600
        CedarBrand.Green -> CedarPalette.Green700
    }
    val primaryContainer = when (brand) {
        CedarBrand.Blue -> CedarPalette.Blue100
        CedarBrand.Green -> CedarPalette.Green100
    }
    val onPrimaryContainer = when (brand) {
        CedarBrand.Blue -> CedarPalette.Blue700
        CedarBrand.Green -> CedarPalette.Green700
    }
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,

        secondary = CedarPalette.Green500,
        onSecondary = CedarPalette.Ink900,
        secondaryContainer = CedarPalette.Green100,
        onSecondaryContainer = CedarPalette.Green700,

        tertiary = CedarPalette.Blue700,
        onTertiary = Color.White,

        background = CedarPalette.Canvas,
        onBackground = CedarPalette.Ink900,

        surface = CedarPalette.Surface,
        onSurface = CedarPalette.Ink900,

        surfaceVariant = CedarPalette.SurfaceSubtle,
        onSurfaceVariant = CedarPalette.Ink500,

        surfaceContainerLowest = CedarPalette.Surface,
        surfaceContainerLow = CedarPalette.SurfaceSubtle,
        surfaceContainer = CedarPalette.Canvas,

        outline = CedarPalette.OutlineStrong,
        outlineVariant = CedarPalette.Outline,

        error = CedarPalette.Red600,
        onError = Color.White,
        errorContainer = CedarPalette.Red100,
        onErrorContainer = CedarPalette.Red900,

        scrim = CedarPalette.Ink900,
    )
}

internal fun cedarDarkColorScheme(brand: CedarBrand): ColorScheme {
    val primary = when (brand) {
        CedarBrand.Blue -> CedarPalette.Blue400
        CedarBrand.Green -> CedarPalette.Green400
    }
    val primaryContainer = when (brand) {
        CedarBrand.Blue -> CedarPalette.Blue900
        CedarBrand.Green -> CedarPalette.Green900
    }
    val onPrimaryContainer = when (brand) {
        CedarBrand.Blue -> CedarPalette.Blue400
        CedarBrand.Green -> CedarPalette.Green400
    }
    return darkColorScheme(
        primary = primary,
        onPrimary = CedarPalette.Ink900,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,

        secondary = CedarPalette.Green400,
        onSecondary = CedarPalette.Ink900,
        secondaryContainer = CedarPalette.Green900,
        onSecondaryContainer = CedarPalette.Green400,

        tertiary = CedarPalette.Blue400,
        onTertiary = CedarPalette.Ink900,

        background = CedarPalette.CanvasDark,
        onBackground = CedarPalette.InkDark900,

        surface = CedarPalette.SurfaceDark,
        onSurface = CedarPalette.InkDark900,

        surfaceVariant = CedarPalette.SurfaceSubtleDark,
        onSurfaceVariant = CedarPalette.InkDark500,

        surfaceContainerLowest = CedarPalette.CanvasDark,
        surfaceContainerLow = CedarPalette.SurfaceDark,
        surfaceContainer = CedarPalette.SurfaceSubtleDark,

        outline = CedarPalette.OutlineStrongDark,
        outlineVariant = CedarPalette.OutlineDark,

        error = CedarPalette.Red400,
        onError = CedarPalette.Ink900,
        errorContainer = CedarPalette.Red900,
        onErrorContainer = CedarPalette.Red100,

        scrim = Color.Black,
    )
}
