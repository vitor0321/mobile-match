package com.walcker.match.cedar.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

public typealias AdBannerContent = @Composable ((Boolean) -> Unit) -> Unit

/**
 * Composition local that provides the platform-specific ad banner composable.
 *
 * Provide a real implementation at the app root via `CompositionLocalProvider`:
 * ```
 * CompositionLocalProvider(LocalAdBannerContent provides { onVisibilityChanged ->
 *     AdMobBannerView(adUnitId, onVisibilityChanged)
 * }) { ... }
 * ```
 * When null (default), [CedarAdBanner] collapses the reserved space.
 */
public val LocalAdBannerContent: ProvidableCompositionLocal<AdBannerContent?> =
    compositionLocalOf { null }