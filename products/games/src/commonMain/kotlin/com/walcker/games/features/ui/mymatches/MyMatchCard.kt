package com.walcker.games.features.ui.mymatches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.model.MatchRole
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.repository.MyMatch
import com.walcker.match.core.datetime.formatWhen

/**
 * Compact card used in My Matches: same skeleton as [MatchCard] but with a role
 * badge and an action button appropriate to the user's relationship with the
 * match (cancel if organizer, leave if participant).
 */
@Composable
internal fun MyMatchCard(
    myMatch: MyMatch,
    organizerBadge: String,
    participantBadge: String,
    cancelActionLabel: String,
    leaveActionLabel: String,
    statusCancelledLabel: String,
    statusFinishedLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = myMatch.game
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = game.sport.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                val roleLabel = if (myMatch.role == MatchRole.ORGANIZER) organizerBadge else participantBadge
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(roleLabel) },
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${game.venueName} · ${game.neighborhood}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formatWhen(startsAtSeconds = game.startsAtSeconds),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${game.confirmedPlayers}/${game.totalPlayers}",
                style = MaterialTheme.typography.bodyMedium,
            )
            val statusLabel = when (game.status) {
                MatchStatus.CANCELLED -> statusCancelledLabel
                MatchStatus.FINISHED -> statusFinishedLabel
                else -> null
            }
            statusLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onActionClick) {
                    Text(
                        text = when (myMatch.role) {
                            MatchRole.ORGANIZER -> cancelActionLabel
                            MatchRole.PARTICIPANT -> leaveActionLabel
                        }
                    )
                }
            }
        }
    }
}
