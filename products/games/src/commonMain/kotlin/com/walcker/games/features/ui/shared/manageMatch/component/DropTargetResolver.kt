package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

internal fun resolveDropTarget(
    dropPoint: Offset,
    sectionBounds: Map<Int, Rect>,
): Int? = sectionBounds.entries.firstOrNull { it.value.contains(dropPoint) }?.key
