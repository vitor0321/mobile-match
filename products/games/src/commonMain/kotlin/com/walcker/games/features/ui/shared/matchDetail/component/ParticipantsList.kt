package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.games.strings.ReportStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun ParticipantsList(
    participants: ParticipantsSummary,
    detail: MatchDetailStrings,
    canRate: Boolean,
    currentUserId: String?,
    reportStrings: ReportStrings,
    onReportPlayer: (userId: String, displayName: String) -> Unit,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        if (participants.confirmed.isNotEmpty()) {
            Text(
                text = detail.confirmedSection(participants.confirmed.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            participants.confirmed.forEach { participant ->
                ParticipantRow(
                    participant = participant,
                    statusLabel = detail.confirmedTag,
                    paidLabel = detail.paidTag,
                    rateLabel = detail.rateAction,
                    canRate = canRate,
                    canReport = participant.userId != currentUserId,
                    reportStrings = reportStrings,
                    onReportPlayer = onReportPlayer,
                    onRatePlayer = onRatePlayer,
                )
            }
        }

        if (participants.waitlist.isNotEmpty()) {
            Text(
                text = detail.waitlistSection(participants.waitlist.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = CedarTokens.spacing.xs),
            )
            participants.waitlist.forEach { participant ->
                ParticipantRow(
                    participant = participant,
                    statusLabel = detail.queuePosition(participant.positionInWaitlist ?: 0),
                    paidLabel = detail.paidTag,
                    rateLabel = detail.rateAction,
                    canRate = false,
                    canReport = participant.userId != currentUserId,
                    reportStrings = reportStrings,
                    onReportPlayer = onReportPlayer,
                    onRatePlayer = onRatePlayer,
                )
            }
        }
    }
}
