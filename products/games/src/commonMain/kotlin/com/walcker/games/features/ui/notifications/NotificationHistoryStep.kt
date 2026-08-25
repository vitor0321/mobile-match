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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walcker.games.features.data.model.NotificationHistoryItem
import com.walcker.games.strings.NotificationHistoryStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val UnreadDotSize = 8.dp

/**
 * Notification history, in a bottom sheet.
 *
 * Three things were wrong beyond the styling:
 *
 * - **The ✕ in the header called `Refresh`.** Tapping the close button reloaded the
 *   list and left the sheet open. It closes the sheet now.
 * - **Relative times were hardcoded English** — "2 hours ago" on a pt-BR screen.
 * - **`onDelete` was passed down and never used**, so `NotificationHistoryEvent.Delete`
 *   was unreachable from the UI. There is a delete button now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryStep(
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    val stepModel: NotificationHistoryStepModel = koinInject()
    val deepLinkCoordinator: DeepLinkCoordinator = koinInject()
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
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NotificationHistoryContent(
                state = state,
                strings = strings,
                onEvent = stepModel::onEvent,
                onClose = {
                    coroutineScope.launch { sheetState.hide() }
                    onDismiss()
                },
                onNotificationTap = { notificationId ->
                    val matchId = state.notifications
                        .find { it.id == notificationId }
                        ?.data
                        ?.get("matchId")
                    if (!matchId.isNullOrEmpty()) {
                        deepLinkCoordinator.navigate(DeepLink.OpenMatch(matchId))
                        coroutineScope.launch { sheetState.hide() }
                        onDismiss()
                    }
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
    onClose: () -> Unit,
    onNotificationTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.sm,
            ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CedarSectionHeader(
                title = strings.title,
                subtitle = if (state.isLoading) strings.refreshing else null,
                modifier = Modifier.weight(1f),
            )
            // Was a TextButton labelled "✕" wired to Refresh.
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = strings.closeContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.errorMessage?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CedarTokens.radius.smShape,
                    )
                    .padding(start = CedarTokens.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onEvent(NotificationHistoryEvent.DismissError) }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.dismissErrorContentDescription,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        if (state.notifications.isEmpty() && !state.isLoading) {
            EmptyState(
                message = strings.emptyState,
                supportingText = strings.noNotifications,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = CedarTokens.spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
            ) {
                items(
                    items = state.notifications,
                    key = { it.id },
                    contentType = { "notification" },
                ) { notification ->
                    NotificationItemRow(
                        notification = notification,
                        strings = strings,
                        onTap = { onNotificationTap(notification.id) },
                        onMarkAsRead = {
                            onEvent(NotificationHistoryEvent.MarkAsRead(notification.id))
                        },
                        onDelete = { onEvent(NotificationHistoryEvent.Delete(notification.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: NotificationHistoryItem,
    strings: NotificationHistoryStrings,
    onTap: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (notification.isRead) {
                    CedarTokens.colors.canvas
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                shape = CedarTokens.radius.smShape,
            )
            .clickable(role = Role.Button, onClick = onTap)
            .padding(start = CedarTokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!notification.isRead) {
            Box(
                modifier = Modifier
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
            modifier = Modifier
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

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 86_400_000L
private const val WEEK_MS = 604_800_000L

/**
 * Relative time for a notification. The labels come from [strings]; the maths does
 * not need translating, the words do.
 */
internal fun formatTimeAgo(
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

/**
 * Current time in milliseconds.
 *
 * Stays here — `MatchDetailStepModel` imports it from this package, and the
 * `actual`s live in `androidMain`/`iosMain` next to this declaration.
 */
internal expect fun getCurrentTimeMillis(): Long
