package com.walcker.games.features.ui.myMatches.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.repository.MyMatch
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarTag
import com.walcker.match.cedar.components.CedarTagTone
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatWhen

@Composable
internal fun MyMatchCard(
    myMatch: MyMatch,
    organizerBadge: String,
    participantBadge: String,
    cancelActionLabel: String,
    leaveActionLabel: String,
    statusCancelledLabel: String,
    statusFinishedLabel: String,
    playersLabel: String,
    isPast: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = myMatch.game
    val statusLabel =
        when {
            game.status == MatchStatus.CANCELLED -> statusCancelledLabel
            game.status == MatchStatus.FINISHED || isPast -> statusFinishedLabel
            else -> null
        }

    Card(
        shape = CedarTokens.radius.mdShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                ) {
                    Text(
                        text = game.venueName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatWhen(startsAtSeconds = game.startsAtSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${game.sport.label} · ${game.neighborhood} · $playersLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                ) {
                    CedarTag(
                        label =
                            if (myMatch.role == MatchRole.ORGANIZER) {
                                organizerBadge
                            } else {
                                participantBadge
                            },
                    )
                    if (statusLabel != null) {
                        CedarTag(
                            label = statusLabel,
                            tone =
                                if (game.status == MatchStatus.CANCELLED) {
                                    CedarTagTone.Danger
                                } else {
                                    CedarTagTone.Neutral
                                },
                        )
                    }
                }
            }

            if (statusLabel == null) {
                CedarSecondaryButton(
                    text =
                        when (myMatch.role) {
                            MatchRole.ORGANIZER -> cancelActionLabel
                            MatchRole.PARTICIPANT -> leaveActionLabel
                        },
                    onClick = onActionClick,
                )
            }
        }
    }
}
