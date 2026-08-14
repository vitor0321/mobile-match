package com.walcker.match.cedar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Compact badge showing how many slots are still open in a match.
 *
 * - Green when openSlots > 0 (accepting more players)
 * - Muted surface color when full
 */
@Composable
public fun SlotBadge(
    openSlots: Int,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = if (openSlots > 0) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = if (openSlots == 0) "LOTADO" else "$openSlots ${if (openSlots == 1) "vaga" else "vagas"}"

    Text(
        text = text,
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(color = bg, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
