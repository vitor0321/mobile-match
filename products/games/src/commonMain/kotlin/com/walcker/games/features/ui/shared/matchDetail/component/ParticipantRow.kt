package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.strings.ReportStrings
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun ParticipantRow(
    participant: Participant,
    statusLabel: String,
    paidLabel: String,
    rateLabel: String,
    canRate: Boolean,
    canReport: Boolean,
    reportStrings: ReportStrings,
    onReportPlayer: (userId: String, displayName: String) -> Unit,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CedarTokens.radius.smShape,
                ).padding(
                    horizontal = CedarTokens.spacing.sm,
                    vertical = CedarTokens.spacing.xs,
                ),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(
            displayName = participant.displayName,
            photoUrl = participant.photoUrl,
            size = PlayerAvatarSize.Small,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (participant.hasPaid) "$statusLabel · $paidLabel" else statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (canRate) {
            TextButton(
                onClick = { onRatePlayer(participant.userId, participant.displayName) },
            ) {
                Text(text = rateLabel, style = MaterialTheme.typography.labelLarge)
            }
        }

        if (canReport) {
            IconButton(
                onClick = { onReportPlayer(participant.userId, participant.displayName) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = reportStrings.reportAction,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
