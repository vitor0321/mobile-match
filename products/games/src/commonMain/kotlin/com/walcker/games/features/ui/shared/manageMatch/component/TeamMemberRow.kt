package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.tokens.CedarTokens

private val MinTouchTarget = 48.dp

@Composable
internal fun TeamMemberRow(
    participant: Participant,
    otherTeamIndices: List<Int>,
    strings: ManageMatchStrings,
    isSaving: Boolean,
    onMoveToTeam: (Int) -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMoveMenu by remember { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface, shape = CedarTokens.radius.smShape)
                .combinedClickable(
                    enabled = !isSaving,
                    onLongClick = { showMoveMenu = true },
                    onLongClickLabel = strings.teamsMoveToMenuTitle,
                    onClick = {},
                ).padding(horizontal = CedarTokens.spacing.sm, vertical = CedarTokens.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
                    .pointerInput(participant.userId, isSaving) {
                        if (isSaving) return@pointerInput
                        detectDragGestures(
                            onDrag = { change, dragAmount -> change.consume(); onDragBy(dragAmount) },
                            onDragEnd = onDragEnd,
                        )
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = strings.teamsDragHandleContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        PlayerAvatar(displayName = participant.displayName, photoUrl = participant.photoUrl, size = PlayerAvatarSize.Small)
        Text(
            text = participant.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
            otherTeamIndices.forEach { teamIndex ->
                DropdownMenuItem(
                    text = { Text(strings.teamsMoveToOption(strings.teamLabel(teamIndex))) },
                    enabled = !isSaving,
                    onClick = {
                        showMoveMenu = false
                        onMoveToTeam(teamIndex)
                    },
                )
            }
        }
    }
}
