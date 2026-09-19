package com.walcker.match.cedar.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class CedarElevation(
    val flat: Dp = 0.dp,
    val raised: Dp = 2.dp,
    val overlay: Dp = 8.dp,
)
