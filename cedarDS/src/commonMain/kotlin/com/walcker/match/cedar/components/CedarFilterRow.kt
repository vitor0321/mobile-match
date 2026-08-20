package com.walcker.match.cedar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

private val RowMinHeight = 56.dp
private val ChevronSize = 20.dp

/**
 * A filter field: a label, the value currently chosen, and a chevron.
 *
 * The Figma draws these as white boxes with text in them, which reads as a text
 * input — during the critique this was the second-most-likely thing for a user to
 * tap expecting a keyboard. The chevron is the fix: it says "this opens something".
 *
 * @param value what is selected right now, e.g. "Hoje · 18:00–22:00". Pass
 *   [placeholder] instead when nothing is selected, so the empty state reads as
 *   muted rather than as a real choice.
 */
@Composable
public fun CedarFilterRow(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = RowMinHeight)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CedarTokens.radius.smShape,
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = CedarTokens.spacing.md),
        ) {
            Text(
                text = value ?: placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = if (value != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ChevronSize),
            )
        }
    }
}

/**
 * A filter whose options are few enough to show at once — the sport picker.
 *
 * Chips instead of a row that opens a sheet: with ten sports and one tap to choose,
 * a sheet would be two extra taps for nothing.
 */
@Composable
public fun CedarFilterSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
