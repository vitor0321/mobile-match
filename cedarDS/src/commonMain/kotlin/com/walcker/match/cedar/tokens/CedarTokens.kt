package com.walcker.match.cedar.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

public object CedarTokens {
    public val colors: CedarColors
        @Composable @ReadOnlyComposable
        get() = LocalCedarColors.current

    public val spacing: CedarSpacing
        @Composable @ReadOnlyComposable
        get() = LocalCedarSpacing.current

    public val radius: CedarRadius
        @Composable @ReadOnlyComposable
        get() = LocalCedarRadius.current

    public val elevation: CedarElevation
        @Composable @ReadOnlyComposable
        get() = LocalCedarElevation.current
}

@Composable
public fun ProvideCedarTokens(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCedarColors provides if (darkTheme) CedarDarkColors else CedarLightColors,
        LocalCedarSpacing provides CedarSpacing(),
        LocalCedarRadius provides CedarRadius(),
        LocalCedarElevation provides CedarElevation(),
        content = content,
    )
}

internal val LocalCedarColors = staticCompositionLocalOf { CedarLightColors }
internal val LocalCedarSpacing = staticCompositionLocalOf { CedarSpacing() }
internal val LocalCedarRadius = staticCompositionLocalOf { CedarRadius() }
internal val LocalCedarElevation = staticCompositionLocalOf { CedarElevation() }
