package com.walcker.games.features.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walcker.games.features.data.model.NotificationHistoryItem
import com.walcker.games.strings.NotificationHistoryStrings
import com.walcker.games.strings.rememberGamesStrings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable for displaying notification history in a ModalBottomSheet.
 * Shows list of notifications with timestamps, read status, and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryStep(
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    val stepModel: NotificationHistoryStepModel = koinInject()
    val state by stepModel.state.collectAsState()
    val strings = rememberGamesStrings().strings.notificationHistory
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch { sheetState.hide() }
                onDismiss()
            },
            sheetState = sheetState,
        ) {
            NotificationHistoryContent(
                state = state,
                strings = strings,
                onEvent = stepModel::onEvent,
                onNotificationTap = { _ ->
                    // TODO Phase 3-ETAPA3: deeplink to match detail
                },
            )
        }
    }
}

@Composable
private fun NotificationHistoryContent(
    state: NotificationHistoryState,
    strings: NotificationHistoryStrings,
    onEvent: (NotificationHistoryEvent) -> Unit,
    onNotificationTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onEvent(NotificationHistoryEvent.Refresh) }) {
                Text("✕", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        state.errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { onEvent(NotificationHistoryEvent.DismissError) },
                    ) {
                        Text("✕", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Content
        if (state.notifications.isEmpty() && !state.isLoading) {
            EmptyState(strings = strings)
        } else {
            NotificationsList(
                notifications = state.notifications,
                isLoading = state.isLoading,
                strings = strings,
                onNotificationTap = onNotificationTap,
                onMarkAsRead = { id ->
                    onEvent(NotificationHistoryEvent.MarkAsRead(id))
                },
                onDelete = { id ->
                    onEvent(NotificationHistoryEvent.Delete(id))
                },
            )
        }
    }
}

@Composable
private fun EmptyState(
    strings: NotificationHistoryStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = strings.noNotifications,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationsList(
    notifications: List<NotificationHistoryItem>,
    isLoading: Boolean,
    strings: NotificationHistoryStrings,
    onNotificationTap: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = notifications,
            key = { it.id },
            contentType = { "notification" },
        ) { notification: NotificationHistoryItem ->
            NotificationItemRow(
                notification = notification,
                onTap = { onNotificationTap(notification.id) },
                onMarkAsRead = { onMarkAsRead(notification.id) },
                onDelete = { onDelete(notification.id) },
            )
        }

        if (isLoading) {
            item {
                Text(
                    text = strings.refreshing,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: NotificationHistoryItem,
    onTap: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (notification.isRead)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onTap)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Unread indicator
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                    .align(Alignment.CenterVertically),
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                text = formatTimeAgo(notification.receivedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Action: Mark as read
        if (!notification.isRead) {
            TextButton(onClick = onMarkAsRead) {
                Text("✓", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Formats a timestamp as relative time (e.g., "2 hours ago").
 */
internal fun formatTimeAgo(timestamp: Long): String {
    val now = getCurrentTimeMillis()
    val diffMs = now - timestamp

    return when {
        diffMs < 60_000 -> "Just now"
        diffMs < 3_600_000 -> "${diffMs / 60_000} minutes ago"
        diffMs < 86_400_000 -> "${diffMs / 3_600_000} hours ago"
        diffMs < 604_800_000 -> "${diffMs / 86_400_000} days ago"
        else -> "${diffMs / 604_800_000} weeks ago"
    }
}

/**
 * Returns current time in milliseconds.
 * Platform-specific implementation needed.
 */
internal expect fun getCurrentTimeMillis(): Long
