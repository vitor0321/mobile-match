package com.walcker.games.features.ui.shared.notifications.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.games.features.ui.shared.notifications.getCurrentTimeMillis
import com.walcker.games.strings.NotificationHistoryStrings
import com.walcker.match.cedar.tokens.CedarTokens

private val UnreadDotSize = 8.dp
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 86_400_000L
private const val WEEK_MS = 604_800_000L

@Composable
internal fun NotificationItemRow(
    notification: NotificationHistoryItem,
    strings: NotificationHistoryStrings,
    onTap: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color =
                        if (notification.isRead) {
                            CedarTokens.colors.canvas
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    shape = CedarTokens.radius.smShape,
                ).clickable(role = Role.Button, onClick = onTap)
                .padding(start = CedarTokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!notification.isRead) {
            Box(
                modifier =
                    Modifier
                        .size(UnreadDotSize)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
            )
        } else {
            Spacer(modifier = Modifier.size(UnreadDotSize))
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(vertical = CedarTokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = formatTimeAgo(timestamp = notification.receivedAt, strings = strings),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!notification.isRead) {
            IconButton(onClick = onMarkAsRead) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = strings.markAsReadContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = strings.deleteContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTimeAgo(
    timestamp: Long,
    strings: NotificationHistoryStrings,
): String {
    val diffMs = (getCurrentTimeMillis() - timestamp).coerceAtLeast(0L)
    return when {
        diffMs < MINUTE_MS -> strings.timeJustNow
        diffMs < HOUR_MS -> strings.timeMinutesAgo((diffMs / MINUTE_MS).toInt())
        diffMs < DAY_MS -> strings.timeHoursAgo((diffMs / HOUR_MS).toInt())
        diffMs < WEEK_MS -> strings.timeDaysAgo((diffMs / DAY_MS).toInt())
        else -> strings.timeWeeksAgo((diffMs / WEEK_MS).toInt())
    }
}
