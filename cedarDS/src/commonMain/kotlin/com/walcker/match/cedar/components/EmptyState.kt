package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

private val IconSize = 48.dp

/**
 * The message a list shows when it has nothing to show.
 *
 * The parameter list grew so the two private copies living in `MyMatchesStep` and
 * `NotificationHistoryStep` can be deleted: one of them needed a subtitle, the
 * other an icon, and neither could use the design system version as it was.
 *
 * An empty state that only says "nothing here" leaves the user stuck. Where there
 * is a next step — widen the radius, clear the filters, create a match — pass
 * [actionLabel] and give them the way out.
 *
 * @param message stays the first parameter, so existing call sites keep compiling.
 */
@Composable
public fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(CedarTokens.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(
            space = CedarTokens.spacing.sm,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (actionLabel != null && onAction != null) {
            CedarSecondaryButton(
                text = actionLabel,
                onClick = onAction,
                fillWidth = false,
            )
        }
    }
}
