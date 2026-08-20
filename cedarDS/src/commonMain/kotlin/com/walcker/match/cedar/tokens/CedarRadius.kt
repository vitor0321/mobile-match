package com.walcker.match.cedar.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radius scale.
 *
 * The Figma uses eight distinct radii (13, 14, 15, 16, 18, 20, 21, 24) with no
 * rule behind them. Normalised here to four steps plus a pill, which reads the
 * same on screen and is one decision instead of eight.
 *
 * Use it as `CedarTokens.radius.md` (a [Dp]) or `CedarTokens.radius.mdShape` (a [Shape]).
 */
@Immutable
public data class CedarRadius(
    /** 12dp — chips, small badges, inputs. Was 13/14 in the Figma. */
    val sm: Dp = 12.dp,
    /** 16dp — list cards, buttons. Was 15/16/18. */
    val md: Dp = 16.dp,
    /** 20dp — hero cards, image containers. Was 18/20/21. */
    val lg: Dp = 20.dp,
    /** 24dp — bottom sheets and the "nearby matches" panel. */
    val xl: Dp = 24.dp,
) {
    val smShape: Shape get() = RoundedCornerShape(sm)
    val mdShape: Shape get() = RoundedCornerShape(md)
    val lgShape: Shape get() = RoundedCornerShape(lg)
    val xlShape: Shape get() = RoundedCornerShape(xl)

    /** Fully rounded — availability pills, filter chips, avatars. */
    val pill: Shape get() = RoundedCornerShape(percent = 50)

    /** Bottom sheet: rounded on top only. */
    val sheet: Shape get() = RoundedCornerShape(topStart = xl, topEnd = xl)
}
