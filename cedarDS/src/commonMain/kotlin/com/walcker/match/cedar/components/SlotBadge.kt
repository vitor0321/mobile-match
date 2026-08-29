package com.walcker.match.cedar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
public fun SlotBadge(
    label: String,
    openSlots: Int,
    modifier: Modifier = Modifier,
) {
    val hasRoom = openSlots > 0
    val background = if (hasRoom) {
        CedarTokens.colors.available
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (hasRoom) {
        CedarTokens.colors.onAvailable
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = label,
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(color = background, shape = CedarTokens.radius.pill)
            .padding(
                horizontal = CedarTokens.spacing.sm,
                vertical = CedarTokens.spacing.xs,
            ),
    )
}
