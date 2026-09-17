package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.tokens.CedarTokens

private const val DRAGGING_SCALE = 1.04f
private val DraggingElevation = 8.dp
private const val TAPPABLE_BADGE_ALPHA = 0.14f

@Composable
internal fun TeamMemberRow(
    participant: Participant,
    otherTeamIndices: List<Int>,
    strings: ManageMatchStrings,
    isSaving: Boolean,
    isDragging: Boolean,
    dragOffset: Offset,
    skillRating: PlayerRatingSummary?,
    myRating: Int?,
    canRateSkill: Boolean,
    onMoveToTeam: (Int) -> Unit,
    onRateSkill: (Int) -> Unit,
    onRowPositioned: (Rect) -> Unit,
    onDragStart: () -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMoveMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (isDragging) DRAGGING_SCALE else 1f, spring())
    val elevation by animateDpAsState(if (isDragging) DraggingElevation else 0.dp, spring())
    val rowShape = CedarTokens.radius.smShape

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates -> onRowPositioned(coordinates.boundsInWindow()) }
                .graphicsLayer {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = elevation.toPx()
                    shape = rowShape
                    clip = false
                }.zIndex(if (isDragging) 1f else 0f)
                .background(color = MaterialTheme.colorScheme.surface, shape = CedarTokens.radius.smShape)
                .padding(horizontal = CedarTokens.spacing.sm, vertical = CedarTokens.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .minimumInteractiveComponentSize()
                    .pointerInput(participant.userId, isSaving) {
                        if (isSaving) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragBy(dragAmount)
                            },
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

        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .combinedClickable(
                        enabled = !isSaving,
                        onLongClick = { showMoveMenu = true },
                        onLongClickLabel = strings.teamsMoveToMenuTitle,
                        onClick = {},
                    ),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(displayName = participant.displayName, photoUrl = participant.photoUrl, size = PlayerAvatarSize.Small)

            PlayerNameAndSkillRating(
                displayName = participant.displayName,
                skillRating = skillRating,
                myRating = myRating,
                canRateSkill = canRateSkill,
                isSaving = isSaving,
                strings = strings,
                onRateSkill = onRateSkill,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Box {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = strings.teamsMoveToMenuTitle,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(enabled = !isSaving) { showMoveMenu = true }
                        .size(20.dp),
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
}

@Composable
private fun PlayerNameAndSkillRating(
    displayName: String,
    skillRating: PlayerRatingSummary?,
    myRating: Int?,
    canRateSkill: Boolean,
    isSaving: Boolean,
    strings: ManageMatchStrings,
    onRateSkill: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rated = skillRating?.takeIf { it.ratingCount > 0 }
    val showSkillLine = rated != null || canRateSkill

    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = MaterialTheme.typography.bodySmall.fontSize),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showSkillLine) {
            val label = rated?.let { strings.skillRatingValue(it.rating) } ?: strings.skillRatingUnrated
            SkillRatingLine(
                label = label,
                myRating = myRating,
                canRateSkill = canRateSkill,
                isSaving = isSaving,
                strings = strings,
                onRateSkill = onRateSkill,
            )
        }
    }
}

@Composable
private fun SkillRatingLine(
    label: String,
    myRating: Int?,
    canRateSkill: Boolean,
    isSaving: Boolean,
    strings: ManageMatchStrings,
    onRateSkill: (Int) -> Unit,
) {
    var showRatingDialog by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier.let { base ->
                if (canRateSkill) {
                    base
                        .minimumInteractiveComponentSize()
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = TAPPABLE_BADGE_ALPHA),
                            shape = CedarTokens.radius.pill,
                        ).clickable(enabled = !isSaving) { showRatingDialog = true }
                        .padding(horizontal = CedarTokens.spacing.xxs)
                } else {
                    base
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = strings.skillRatingContentDescription,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = MaterialTheme.typography.labelSmall.fontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (showRatingDialog) {
        SkillRatingDialog(
            myRating = myRating,
            isSaving = isSaving,
            strings = strings,
            onRateSkill = onRateSkill,
            onDismiss = { showRatingDialog = false },
        )
    }
}
