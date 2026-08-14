package com.walcker.match.cedar.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A filter chip representing a single sport option (or "all").
 *
 * Wraps Material3 [FilterChip] so screens don't need to import it
 * directly and so the call site reads as semantic UI rather than
 * generic chip configuration.
 */
@Composable
public fun SportChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
    )
}
