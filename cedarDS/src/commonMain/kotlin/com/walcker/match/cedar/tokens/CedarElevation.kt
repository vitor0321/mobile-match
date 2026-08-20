package com.walcker.match.cedar.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation scale.
 *
 * The redesign gets its depth from the tinted canvas behind white cards, not from
 * shadows — so the default is genuinely flat. Shadow is reserved for things that
 * float over content: the sheet, the map buttons, the bottom bar.
 *
 * Use it as `CedarTokens.elevation.raised`.
 */
@Immutable
public data class CedarElevation(
    /** 0dp — cards in a list. Contrast comes from surface vs. canvas. */
    val flat: Dp = 0.dp,
    /** 2dp — something that sits above the canvas but not above content. */
    val raised: Dp = 2.dp,
    /** 8dp — floats over content: bottom sheet, map controls, bottom bar. */
    val overlay: Dp = 8.dp,
)
