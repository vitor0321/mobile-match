package com.walcker.match.cedar

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// ─── Color tokens ─────────────────────────────────────────────────────────────

internal object CedarColors {
    // Green brand — light
    val GreenPrimaryLight            = Color(0xFF3A9A2C)
    val GreenOnPrimaryLight          = Color(0xFFE8F5E6)
    val GreenPrimaryContainerLight   = Color(0xFFE8F5E6)
    val GreenOnPrimaryContainerLight = Color(0xFF1F6117)

    // Green brand — dark
    val GreenPrimaryDark             = Color(0xFF5DB54F)
    val GreenOnPrimaryDark           = Color(0xFF0C2E08)
    val GreenPrimaryContainerDark    = Color(0xFF132110)
    val GreenOnPrimaryContainerDark  = Color(0xFFC3E6BE)

    // Gold accent (both modes)
    val Gold = Color(0xFFC9A84C)

    // Error
    val ErrorBgLight     = Color(0xFFFAEAEA)
    val ErrorStrokeLight = Color(0xFFC94040)
    val ErrorBgDark      = Color(0xFF2E1515)
    val ErrorStrokeDark  = Color(0xFFE05555)
}

// ─── Color schemes ────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary                = CedarColors.GreenPrimaryLight,
    onPrimary              = CedarColors.GreenOnPrimaryLight,
    primaryContainer       = CedarColors.GreenPrimaryContainerLight,
    onPrimaryContainer     = CedarColors.GreenOnPrimaryContainerLight,
    secondary              = CedarColors.Gold,
    onSecondary            = Color(0xFF2C2B26),
    secondaryContainer     = Color(0xFFF5EDD0),
    onSecondaryContainer   = Color(0xFF3A2F0A),
    background             = Color(0xFFF9F6EE),
    onBackground           = Color(0xFF2C2B26),
    surface                = Color.White,
    onSurface              = Color(0xFF2C2B26),
    surfaceVariant         = Color(0xFFE7F5E3),
    onSurfaceVariant       = Color(0xFF55534C),
    outline                = Color(0xFFEDE8D8),
    outlineVariant         = Color(0xFFD9D1BC),
    error                  = CedarColors.ErrorStrokeLight,
    onError                = Color.White,
    errorContainer         = CedarColors.ErrorBgLight,
    onErrorContainer       = CedarColors.ErrorStrokeLight,
)

private val DarkColorScheme = darkColorScheme(
    primary                = CedarColors.GreenPrimaryDark,
    onPrimary              = CedarColors.GreenOnPrimaryDark,
    primaryContainer       = CedarColors.GreenPrimaryContainerDark,
    onPrimaryContainer     = CedarColors.GreenOnPrimaryContainerDark,
    secondary              = CedarColors.Gold,
    onSecondary            = Color(0xFF0C2E08),
    secondaryContainer     = Color(0xFF2A2008),
    onSecondaryContainer   = Color(0xFFD4A840),
    background             = Color(0xFF0F1A0D),
    onBackground           = Color(0xFFE8F5E6),
    surface                = Color(0xFF1A2618),
    onSurface              = Color(0xFFE8F5E6),
    surfaceVariant         = Color(0xFF243421),
    onSurfaceVariant       = Color(0xFF5F7A5A),
    outline                = Color(0xFF2D4229),
    outlineVariant         = Color(0xFF1F3A1A),
    error                  = CedarColors.ErrorStrokeDark,
    onError                = Color(0xFF2E1515),
    errorContainer         = CedarColors.ErrorBgDark,
    onErrorContainer       = CedarColors.ErrorStrokeDark,
)

// ─── Typography ───────────────────────────────────────────────────────────────

public data class CedarTypography(
    val bodyVerse: TextStyle = TextStyle(
        fontFamily    = FontFamily.Serif,
        fontSize      = 17.sp,
        lineHeight    = 29.75.sp, // 17 * 1.75
        letterSpacing = 0.sp,
    ),
)

internal val LocalCedarTypography = staticCompositionLocalOf { CedarTypography() }

// ─── Theme accessor ───────────────────────────────────────────────────────────

public object CedarTheme {
    public val typography: CedarTypography
        @Composable get() = LocalCedarTypography.current
}

// ─── Theme composable ─────────────────────────────────────────────────────────

@Composable
public fun CedarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCedarTypography provides CedarTypography()) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            content = content,
        )
    }
}
