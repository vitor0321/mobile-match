package com.walcker.match.cedar.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale.
 *
 * Before this existed, `products/` carried 73 literal `16.dp`, 63 `8.dp` and 38
 * `12.dp` — which is how a codebase ends up with margins that drift by 4dp
 * between screens without anyone noticing.
 *
 * Use it as `CedarTokens.spacing.md`.
 */
@Immutable
public data class CedarSpacing(
    /** 4dp — between a label and the value right under it. */
    val xxs: Dp = 4.dp,
    /** 8dp — between related lines inside a card. */
    val xs: Dp = 8.dp,
    /** 12dp — between cards in a list. */
    val sm: Dp = 12.dp,
    /** 16dp — padding inside a card. */
    val md: Dp = 16.dp,
    /** 20dp — screen side margin. Fixed: the Figma drifts between 16, 20 and 24. */
    val lg: Dp = 20.dp,
    /** 24dp — between sections. */
    val xl: Dp = 24.dp,
    /** 32dp — above a bottom CTA, below a screen title. */
    val xxl: Dp = 32.dp,
) {
    /** Side margin of every screen. Alias for [lg], named for the call site. */
    val screenHorizontal: Dp get() = lg
}
