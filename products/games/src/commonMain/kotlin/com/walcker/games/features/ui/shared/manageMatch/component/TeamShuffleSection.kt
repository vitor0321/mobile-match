package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.coroutines.launch

private val TeamAccentWidth = 4.dp
private val TeamAccentHeight = 20.dp

@Composable
internal fun TeamShuffleSection(
    selectedTeamCount: Int,
    selectedPlayersPerTeam: Int,
    teamCount: Int,
    playersPerTeam: Int,
    teamAssignments: Map<String, Int>,
    confirmedPlayers: List<Participant>,
    skillRatings: Map<String, PlayerRatingSummary>,
    mySkillRatings: Map<String, Int>,
    canRateSkill: Boolean,
    strings: ManageMatchStrings,
    isSaving: Boolean,
    onTeamCountSelected: (Int) -> Unit,
    onPlayersPerTeamSelected: (Int) -> Unit,
    onShuffle: () -> Unit,
    onMovePlayer: (userId: String, teamIndex: Int) -> Unit,
    onRateSkill: (userId: String, rating: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
        Text(
            text = strings.teamsCountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TeamCountSelector(selectedCount = selectedTeamCount, onCountSelected = onTeamCountSelected)

        PlayersPerTeamSlider(
            selectedCount = selectedPlayersPerTeam,
            strings = strings,
            enabled = !isSaving,
            onCountSelected = onPlayersPerTeamSelected,
        )

        if (teamCount > 0) {
            TeamsBoard(
                teamCount = teamCount,
                playersPerTeam = playersPerTeam,
                teamAssignments = teamAssignments,
                confirmedPlayers = confirmedPlayers,
                skillRatings = skillRatings,
                mySkillRatings = mySkillRatings,
                canRateSkill = canRateSkill,
                strings = strings,
                isSaving = isSaving,
                onMovePlayer = onMovePlayer,
                onRateSkill = onRateSkill,
            )
        }

        CedarPrimaryButton(
            text = if (teamCount == 0) strings.teamsShuffleAction else strings.teamsReshuffleAction,
            onClick = onShuffle,
            loading = isSaving,
            enabled = confirmedPlayers.isNotEmpty(),
        )
    }
}

@Composable
private fun TeamsBoard(
    teamCount: Int,
    playersPerTeam: Int,
    teamAssignments: Map<String, Int>,
    confirmedPlayers: List<Participant>,
    skillRatings: Map<String, PlayerRatingSummary>,
    mySkillRatings: Map<String, Int>,
    canRateSkill: Boolean,
    strings: ManageMatchStrings,
    isSaving: Boolean,
    onMovePlayer: (userId: String, teamIndex: Int) -> Unit,
    onRateSkill: (userId: String, rating: Int) -> Unit,
) {
    var sectionBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var rowBounds by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var draggingUserId by remember { mutableStateOf<String?>(null) }
    var draggingOrigin by remember { mutableStateOf(Offset.Zero) }
    var hoveredTeamIndex by remember { mutableStateOf<Int?>(null) }
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    fun beginDrag(userId: String) {
        draggingUserId = userId
        draggingOrigin = rowBounds[userId]?.center ?: Offset.Zero
        hoveredTeamIndex = resolveDropTarget(draggingOrigin, sectionBounds)
    }

    fun dragBy(delta: Offset) {
        val updated = dragOffset.value + delta
        scope.launch { dragOffset.snapTo(updated) }
        hoveredTeamIndex = resolveDropTarget(draggingOrigin + updated, sectionBounds)
    }

    fun endDrag(originTeamIndex: Int?) {
        val userId = draggingUserId ?: return
        val targetTeam = resolveDropTarget(draggingOrigin + dragOffset.value, sectionBounds)
        if (targetTeam != null && targetTeam != originTeamIndex) {
            onMovePlayer(userId, targetTeam)
            scope.launch { dragOffset.snapTo(Offset.Zero) }
        } else {
            scope.launch { dragOffset.animateTo(Offset.Zero, spring()) }
        }
        draggingUserId = null
        hoveredTeamIndex = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md)) {
        for (teamIndex in 0 until teamCount) {
            val members = confirmedPlayers.filter { teamAssignments[it.userId] == teamIndex }
            val isHovered = hoveredTeamIndex == teamIndex && draggingUserId != null
            val sectionBackground by animateColorAsState(
                if (isHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
            )
            val accentColor =
                if (teamIndex % 2 == 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            sectionBounds = sectionBounds + (teamIndex to coordinates.boundsInWindow())
                        }.background(color = sectionBackground, shape = CedarTokens.radius.smShape)
                        .padding(CedarTokens.spacing.xxs),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(width = TeamAccentWidth, height = TeamAccentHeight)
                                .background(color = accentColor, shape = CedarTokens.radius.pill),
                    )
                    Text(
                        text = strings.teamLabel(teamIndex).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = strings.teamsCapacityLabel(members.size, playersPerTeam),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (members.isEmpty()) {
                    Text(
                        text = strings.teamsEmptySection,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                members.forEach { participant ->
                    TeamMemberRow(
                        participant = participant,
                        otherTeamIndices = (0 until teamCount).filterNot { it == teamIndex },
                        strings = strings,
                        isSaving = isSaving,
                        isDragging = draggingUserId == participant.userId,
                        dragOffset = if (draggingUserId == participant.userId) dragOffset.value else Offset.Zero,
                        skillRating = skillRatings[participant.userId],
                        myRating = mySkillRatings[participant.userId],
                        canRateSkill = canRateSkill,
                        onMoveToTeam = { targetIndex -> onMovePlayer(participant.userId, targetIndex) },
                        onRateSkill = { rating -> onRateSkill(participant.userId, rating) },
                        onRowPositioned = { bounds -> rowBounds = rowBounds + (participant.userId to bounds) },
                        onDragStart = { beginDrag(participant.userId) },
                        onDragBy = ::dragBy,
                        onDragEnd = { endDrag(originTeamIndex = teamIndex) },
                    )
                }
            }
        }

        val unassigned = confirmedPlayers.filterNot { teamAssignments.containsKey(it.userId) }
        if (unassigned.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs)) {
                Text(text = strings.teamsUnassignedSection, style = MaterialTheme.typography.titleSmall)
                unassigned.forEach { participant ->
                    TeamMemberRow(
                        participant = participant,
                        otherTeamIndices = (0 until teamCount).toList(),
                        strings = strings,
                        isSaving = isSaving,
                        isDragging = draggingUserId == participant.userId,
                        dragOffset = if (draggingUserId == participant.userId) dragOffset.value else Offset.Zero,
                        skillRating = skillRatings[participant.userId],
                        myRating = mySkillRatings[participant.userId],
                        canRateSkill = canRateSkill,
                        onMoveToTeam = { targetIndex -> onMovePlayer(participant.userId, targetIndex) },
                        onRateSkill = { rating -> onRateSkill(participant.userId, rating) },
                        onRowPositioned = { bounds -> rowBounds = rowBounds + (participant.userId to bounds) },
                        onDragStart = { beginDrag(participant.userId) },
                        onDragBy = ::dragBy,
                        onDragEnd = { endDrag(originTeamIndex = null) },
                    )
                }
            }
        }
    }
}
