package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
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
import com.walcker.match.cedar.components.CedarIcons
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens

private val RateActionSize = 48.dp
private const val DISABLED_ICON_ALPHA = 0.38f

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
    banLabel: String? = null,
    onBanPlayer: ((userId: String, displayName: String) -> Unit)? = null,
    isVip: Boolean = false,
    vipLabel: String? = null,
    onToggleVip: ((userId: String, displayName: String, currentlyVip: Boolean) -> Unit)? = null,
    vipEnabled: Boolean = true,
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

        Box(modifier = Modifier.size(RateActionSize), contentAlignment = Alignment.Center) {
            if (canRate) {
                IconButton(
                    onClick = { onRatePlayer(participant.userId, participant.displayName) },
                ) {
                    Icon(
                        imageVector = if (alreadyRated) Icons.Filled.RateReview else Icons.Outlined.RateReview,
                        contentDescription = rateLabel,
                        tint = if (alreadyRated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (onBanPlayer != null && banLabel != null) {
            Box(modifier = Modifier.size(RateActionSize), contentAlignment = Alignment.Center) {
                IconButton(onClick = { onBanPlayer(participant.userId, participant.displayName) }) {
                    Icon(
                        imageVector = Icons.Default.PersonRemove,
                        contentDescription = banLabel,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (onToggleVip != null && vipLabel != null) {
            Box(modifier = Modifier.size(RateActionSize), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { onToggleVip(participant.userId, participant.displayName, isVip) },
                    enabled = vipEnabled,
                ) {
                    Icon(
                        imageVector = CedarIcons.Crown,
                        contentDescription = vipLabel,
                        tint =
                            (if (isVip) CedarTokens.colors.vip else MaterialTheme.colorScheme.onSurfaceVariant)
                                .copy(alpha = if (vipEnabled) 1f else DISABLED_ICON_ALPHA),
                    )
                }
            }
        }
    }
}
