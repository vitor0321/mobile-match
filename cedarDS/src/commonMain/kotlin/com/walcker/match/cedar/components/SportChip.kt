package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

/** Minimum touch target. The chip reads as 42dp in the design; it must not *be* 42dp. */
private val MinTouchTarget = 48.dp

/**
 * A filter chip for a single sport, or for "all".
 *
 * The redesign draws these as white pills that fill with the brand colour when
 * selected — flatter than the Material default, which leans on a border and a
 * checkmark.
 */
@Composable
public fun SportChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        shape = CedarTokens.radius.pill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        // No border: the redesign draws these as flat white pills. The Material
        // default border would fight the tinted canvas behind them.
        border = null,
        modifier = modifier.defaultMinSize(minHeight = MinTouchTarget),
    )
}
