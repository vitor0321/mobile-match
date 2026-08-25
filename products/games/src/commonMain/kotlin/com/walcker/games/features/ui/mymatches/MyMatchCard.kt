package com.walcker.games.features.ui.mymatches

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
import com.walcker.games.features.domain.model.MatchRole
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.repository.MyMatch
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarTag
import com.walcker.match.cedar.components.CedarTagTone
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatWhen

/**
 * A match in "Minhas partidas": the same skeleton as `MatchCard`, plus the user's
 * role and the action that belongs to it (cancel if organiser, leave if participant).
 *
 * Two fixes beyond the visuals:
 * - It rendered `game.sport.name` — the enum constant, so the card said `FUTEBOL`
 *   while every other screen said `Futebol`. Same bug the map had.
 * - The role badge was a **disabled `AssistChip`**, which a screen reader announces
 *   as a disabled button. It is a label, so it is a label now.
 *
 * @param isPast the card came from the Passadas tab. It is the only source of
 *   "it's over": `status` never becomes [MatchStatus.FINISHED], so without this a
 *   yesterday's match showed no label at all. `GetMyMatchesUseCase` makes the cut
 *   by the clock.
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
    playersLabel: String,
    isPast: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = myMatch.game
    val statusLabel = when {
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
                        label = if (myMatch.role == MatchRole.ORGANIZER) {
                            organizerBadge
                        } else {
                            participantBadge
                        },
                    )
                    if (statusLabel != null) {
                        CedarTag(
                            label = statusLabel,
                            tone = if (game.status == MatchStatus.CANCELLED) {
                                CedarTagTone.Danger
                            } else {
                                CedarTagTone.Neutral
                            },
                        )
                    }
                }
            }

            // A finished or cancelled match has nothing left to cancel or leave.
            if (statusLabel == null) {
                CedarSecondaryButton(
                    text = when (myMatch.role) {
                        MatchRole.ORGANIZER -> cancelActionLabel
                        MatchRole.PARTICIPANT -> leaveActionLabel
                    },
                    onClick = onActionClick,
                )
            }
        }
    }
}
