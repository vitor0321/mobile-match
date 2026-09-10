package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun TeamShuffleSection(
    selectedTeamCount: Int,
    teamCount: Int,
    teamAssignments: Map<String, Int>,
    confirmedPlayers: List<Participant>,
    strings: ManageMatchStrings,
    isSaving: Boolean,
    onTeamCountSelected: (Int) -> Unit,
    onShuffle: () -> Unit,
    onMovePlayer: (userId: String, teamIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
        CedarSectionHeader(title = strings.teamsSectionTitle)

        Text(
            text = strings.teamsCountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TeamCountSelector(selectedCount = selectedTeamCount, onCountSelected = onTeamCountSelected)

        if (teamCount > 0) {
            TeamsBoard(
                teamCount = teamCount,
                teamAssignments = teamAssignments,
                confirmedPlayers = confirmedPlayers,
                strings = strings,
                isSaving = isSaving,
                onMovePlayer = onMovePlayer,
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
    teamAssignments: Map<String, Int>,
    confirmedPlayers: List<Participant>,
    strings: ManageMatchStrings,
    isSaving: Boolean,
    onMovePlayer: (userId: String, teamIndex: Int) -> Unit,
) {
    var sectionBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var draggingUserId by remember { mutableStateOf<String?>(null) }
    var draggingOrigin by remember { mutableStateOf(Offset.Zero) }
    var draggingCurrent by remember { mutableStateOf(Offset.Zero) }

    Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md)) {
        for (teamIndex in 0 until teamCount) {
            val members = confirmedPlayers.filter { teamAssignments[it.userId] == teamIndex }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            sectionBounds = sectionBounds + (teamIndex to coordinates.boundsInWindow())
                        },
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
            ) {
                Text(text = strings.teamLabel(teamIndex), style = MaterialTheme.typography.titleSmall)
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
                        onMoveToTeam = { targetIndex -> onMovePlayer(participant.userId, targetIndex) },
                        onDragBy = { delta ->
                            draggingUserId = participant.userId
                            draggingOrigin = sectionBounds[teamIndex]?.topLeft ?: Offset.Zero
                            draggingCurrent += delta
                        },
                        onDragEnd = {
                            val userId = draggingUserId ?: return@TeamMemberRow
                            val dropPoint = draggingOrigin + draggingCurrent
                            val targetTeam = sectionBounds.entries.firstOrNull { it.value.contains(dropPoint) }?.key
                            if (targetTeam != null && targetTeam != teamIndex) {
                                onMovePlayer(userId, targetTeam)
                            }
                            draggingUserId = null
                            draggingCurrent = Offset.Zero
                        },
                    )
                }
            }
        }

        val unassigned = confirmedPlayers.filterNot { teamAssignments.containsKey(it.userId) }
        if (unassigned.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs)) {
                Text(text = strings.teamsUnassignedSection, style = MaterialTheme.typography.titleSmall)
                unassigned.forEach { participant ->
                    TeamMemberRow(
                        participant = participant,
                        otherTeamIndices = (0 until teamCount).toList(),
                        strings = strings,
                        isSaving = isSaving,
                        onMoveToTeam = { targetIndex -> onMovePlayer(participant.userId, targetIndex) },
                        onDragBy = {},
                        onDragEnd = {},
                    )
                }
            }
        }
    }
}
