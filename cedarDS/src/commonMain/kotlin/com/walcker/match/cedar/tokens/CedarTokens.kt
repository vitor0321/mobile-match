package com.walcker.match.cedar.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How components reach the design tokens.
 *
 * ```
 * Modifier.padding(CedarTokens.spacing.md)
 * Card(shape = CedarTokens.radius.mdShape)
 * Text(color = CedarTokens.colors.availableText)
 * ```
 *
 * Material 3 slots (`primary`, `surface`, `onSurface`, …) stay on
 * `MaterialTheme.colorScheme`. Only what Material has no slot for lives here.
 *
 * Every local has a working default, so a component renders correctly even outside
 * `CedarTheme` — in a preview, a screenshot test, or while the app is mid-migration.
 * The one thing the defaults cannot know is dark mode: [colors] falls back to the
 * light palette until `CedarTheme` provides otherwise.
 */
public object CedarTokens {

    public val colors: CedarColors
        @Composable @ReadOnlyComposable get() = LocalCedarColors.current

    public val spacing: CedarSpacing
        @Composable @ReadOnlyComposable get() = LocalCedarSpacing.current

    public val radius: CedarRadius
        @Composable @ReadOnlyComposable get() = LocalCedarRadius.current

    public val elevation: CedarElevation
        @Composable @ReadOnlyComposable get() = LocalCedarElevation.current
}

/**
 * Publishes the token set. `CedarTheme` calls this; nothing else should need to,
 * except a test that wants to render a component in dark mode on its own.
 */
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
