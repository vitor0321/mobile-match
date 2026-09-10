package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun ParticipantRow(
    participant: Participant,
    statusLabel: String,
    paidLabel: String,
    rateLabel: String,
    alreadyRated: Boolean,
    canRate: Boolean,
    ratingSummary: PlayerRatingSummary?,
    ratingsCountLabel: (Int) -> String,
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
            if (ratingSummary != null && ratingSummary.ratingCount > 0) {
                RatingStars(
                    rating = ratingSummary.rating,
                    starSize = 12.dp,
                    contentDescription = ratingsCountLabel(ratingSummary.ratingCount),
                )
            }
            Text(
                text = if (participant.hasPaid) "$statusLabel · $paidLabel" else statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (canRate) {
            IconButton(
                onClick = { onRatePlayer(participant.userId, participant.displayName) },
            ) {
                Icon(
                    imageVector = if (alreadyRated) Icons.Filled.RateReview else Icons.Outlined.RateReview,
                    contentDescription = rateLabel,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
