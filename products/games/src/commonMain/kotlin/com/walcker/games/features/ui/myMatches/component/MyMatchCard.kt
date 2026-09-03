package com.walcker.games.features.ui.myMatches.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.repository.MyMatch
import com.walcker.match.cedar.components.CedarTag
import com.walcker.match.cedar.components.CedarTagTone
import com.walcker.match.cedar.components.RatingStars
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
    ratingsCountLabel: (Int) -> String,
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
                    if (game.matchRatingCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                        ) {
                            RatingStars(rating = game.matchRating.toFloat(), starSize = 12.dp)
                            Text(
                                text = ratingsCountLabel(game.matchRatingCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
                val isOrganizer = myMatch.role == MatchRole.ORGANIZER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isOrganizer) Arrangement.End else Arrangement.Start,
                ) {
                    Text(
                        text = if (isOrganizer) cancelActionLabel else leaveActionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            if (isOrganizer) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                            },
                        modifier =
                            Modifier
                                .clip(CedarTokens.radius.smShape)
                                .clickable(onClick = onActionClick, role = Role.Button)
                                .defaultMinSize(minHeight = 48.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                                .padding(horizontal = CedarTokens.spacing.sm),
                    )
                }
            }
        }
    }
}
