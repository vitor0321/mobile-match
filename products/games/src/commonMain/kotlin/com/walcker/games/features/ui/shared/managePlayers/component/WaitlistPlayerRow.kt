package com.walcker.games.features.ui.shared.managePlayers.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
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
import com.walcker.match.cedar.components.CedarIcons
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.tokens.CedarTokens

private val WaitlistActionSize = 48.dp

@Composable
internal fun WaitlistPlayerRow(
    participant: Participant,
    positionLabel: String,
    confirmLabel: String,
    banLabel: String,
    onConfirmToGame: (userId: String, displayName: String) -> Unit,
    onBanPlayer: (userId: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier,
    showVip: Boolean = false,
    vipLabel: String? = null,
    onToggleVip: ((userId: String, displayName: String, currentlyVip: Boolean) -> Unit)? = null,
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
                text = positionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showVip && vipLabel != null && onToggleVip != null) {
            Box(modifier = Modifier.size(WaitlistActionSize), contentAlignment = Alignment.Center) {
                IconButton(onClick = { onToggleVip(participant.userId, participant.displayName, participant.isVip) }) {
                    Icon(
                        imageVector = CedarIcons.Crown,
                        contentDescription = vipLabel,
                        tint = if (participant.isVip) CedarTokens.colors.vip else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Box(modifier = Modifier.size(WaitlistActionSize), contentAlignment = Alignment.Center) {
            IconButton(onClick = { onConfirmToGame(participant.userId, participant.displayName) }) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = confirmLabel,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Box(modifier = Modifier.size(WaitlistActionSize), contentAlignment = Alignment.Center) {
            IconButton(onClick = { onBanPlayer(participant.userId, participant.displayName) }) {
                Icon(
                    imageVector = Icons.Filled.PersonRemove,
                    contentDescription = banLabel,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
