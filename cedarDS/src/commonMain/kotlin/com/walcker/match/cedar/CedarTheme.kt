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
    val GreenPrimaryLight            = Color(0xFF06C167)
    val GreenOnPrimaryLight          = Color(0xFFFFFFFF)
    val GreenPrimaryContainerLight   = Color(0xFFE6F7EF)
    val GreenOnPrimaryContainerLight = Color(0xFF002D1B)

    // Green brand — dark
    val GreenPrimaryDark             = Color(0xFF06C167)
    val GreenOnPrimaryDark           = Color(0xFF000000)
    val GreenPrimaryContainerDark    = Color(0xFF003A24)
    val GreenOnPrimaryContainerDark  = Color(0xFFF6CF2A)

    // Gold accent (both modes)
    val Gold = Color(0xFFEEEEEE)

    // Error
    val ErrorBgLight     = Color(0xFFFFFDEB)
    val ErrorStrokeLight = Color(0xFFFDA291)
    val ErrorBgDark      = Color(0xFFF3A1D1B)
    val ErrorStrokeDark  = Color(0xFFFDA291)
}

// ─── Color schemes ────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary                = CedarColors.GreenPrimaryLight,
    onPrimary              = CedarColors.GreenOnPrimaryLight,
    primaryContainer       = CedarColors.GreenPrimaryContainerLight,
    onPrimaryContainer     = CedarColors.GreenOnPrimaryContainerLight,

    secondary              = CedarColors.Gold,
    onSecondary            = Color(0xFF000000),
    secondaryContainer     = Color(0xFFEEEEEE),
    onSecondaryContainer   = Color(0xFF000000),

    background             = Color(0xFFFFFFFF),
    onBackground           = Color(0xFF000000),

    surface                = Color(0xFFFFFFFF),
    onSurface              = Color(0xFF000000),

    surfaceVariant         = Color(0xFFEEEEEE),
    onSurfaceVariant       = Color(0xFFB6B6B6),

    outline                = Color(0xFFE0E0E0),
    outlineVariant         = Color(0xFFEEEEEE),

    error                  = CedarColors.ErrorStrokeLight,
    onError                = Color(0xFFFFFFFF),
    errorContainer         = CedarColors.ErrorBgLight,
    onErrorContainer       = CedarColors.ErrorStrokeLight,
)

private val DarkColorScheme = darkColorScheme(
    primary                = CedarColors.GreenPrimaryDark,
    onPrimary              = CedarColors.GreenOnPrimaryDark,
    primaryContainer       = CedarColors.GreenPrimaryContainerDark,
    onPrimaryContainer     = CedarColors.GreenOnPrimaryContainerDark,

    secondary              = CedarColors.Gold,
    onSecondary            = Color(0xFF000000),
    secondaryContainer     = Color(0xFF1C1C1C),
    onSecondaryContainer   = Color(0xFFEEEEEE),

    background             = Color(0xFF000000),
    onBackground           = Color(0xFFEEEEEE),

    surface                = Color(0xFF1C1C1C),
    onSurface              = Color(0xFFEEEEEE),

    surfaceVariant         = Color(0xFF2A2A2A),
    onSurfaceVariant       = Color(0xFFB6B6B6),

    outline                = Color(0xFF3A3A3A),
    outlineVariant         = Color(0xFF2A2A2A),

    error                  = CedarColors.ErrorStrokeDark,
    onError                = Color(0xFFFFFFFF),
    errorContainer         = CedarColors.ErrorBgDark,
    onErrorContainer       = CedarColors.ErrorStrokeDark,
)

// ─── Typography ───────────────────────────────────────────────────────────────

/**
 * Estilos próprios do Cedar, além do que o Material já dá.
 *
 * Antes daqui só saía `bodyVerse` — serifada, entrelinha de leitura longa —
 * herdado do Lexis e sem uma única chamada no app. Ficou [venueName], que é o
 * texto que de fato se repete no produto: nome de quadra em card de partida,
 * onde truncar cedo é pior do que apertar a fonte.
 *
 * Se a identidade visual for para outro lugar (item "Tema Cedar" da Phase 7),
 * é aqui que ela nasce.
 */
public data class CedarTypography(
    val venueName: TextStyle = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontSize      = 15.sp,
        lineHeight    = 20.sp,
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
