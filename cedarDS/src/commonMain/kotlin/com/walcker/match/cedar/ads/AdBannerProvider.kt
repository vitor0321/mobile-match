package com.walcker.match.cedar.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

public typealias AdBannerContent = @Composable ((Boolean) -> Unit) -> Unit

public val LocalAdBannerContent: ProvidableCompositionLocal<AdBannerContent?> =
    compositionLocalOf { null }