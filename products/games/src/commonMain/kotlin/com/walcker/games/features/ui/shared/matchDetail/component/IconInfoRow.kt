package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

private val IconSize = 20.dp
private val MinTouchTarget = 48.dp

@Composable
internal fun IconInfoRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val tint = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.defaultMinSize(minHeight = MinTouchTarget).clickable(onClick = onClick) else it }
                .semantics(mergeDescendants = true) { if (onClick != null) role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(IconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
        )
    }
}
