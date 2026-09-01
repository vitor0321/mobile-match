package com.walcker.games.features.ui.shared.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.walcker.games.features.ui.shared.notifications.component.NotificationItemRow
import com.walcker.games.strings.NotificationHistoryStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
                    val matchId =
                        state.notifications
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
        modifier =
            modifier
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CedarTokens.radius.smShape,
                        ).padding(start = CedarTokens.spacing.md),
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

internal expect fun getCurrentTimeMillis(): Long
