package com.walcker.match.cedar.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class CedarRadius(
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
) {
    val smShape: Shape get() = RoundedCornerShape(sm)
    val mdShape: Shape get() = RoundedCornerShape(md)
    val lgShape: Shape get() = RoundedCornerShape(lg)
    val xlShape: Shape get() = RoundedCornerShape(xl)

    val pill: Shape get() = RoundedCornerShape(percent = 50)

    val sheet: Shape get() = RoundedCornerShape(topStart = xl, topEnd = xl)
}
