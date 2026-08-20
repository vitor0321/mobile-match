package com.walcker.match.cedar.components

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatWhen

/**
 * A single match in a list.
 *
 * Rebuilt to match the redesign, and the shape changed in two ways that matter:
 *
 * - **The whole card is the target** — once [onClick] is wired. The old version put
 *   an "ENTRAR NO JOGO" button inside every row, which turned each list into a wall
 *   of competing CTAs while the card itself did nothing. The card should open the
 *   match; joining belongs on the detail screen, where the user can see who is
 *   already in.
 * - **The sport is not the headline.** It used to be the first line, in caps. The
 *   venue and the time are what people scan for — the sport is already filtered.
 *
 * Both [onClick] and [joinButtonLabel] are optional on purpose. Neither list screen
 * can navigate to a match detail yet, so this lands with the join button still in
 * the card; wiring `onClick` and dropping the button is the next step, not this one.
 *
 * @param metaLabel one line of context: "Futebol · Vila Mariana".
 * @param slotsLabel already pluralised by the caller: "2 vagas", "Lotado".
 * @param openSlots drives the badge colour, not its text.
 */
@Composable
public fun MatchCard(
    venueName: String,
    startsAtSeconds: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    metaLabel: String? = null,
    priceLabel: String? = null,
    slotsLabel: String? = null,
    openSlots: Int = 0,
    joinButtonLabel: String? = null,
    onJoinClick: (() -> Unit)? = null,
) {
    val shape = CedarTokens.radius.mdShape
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat)
    // One announcement per card instead of five, so a screen reader user hears
    // "Arena Paulista, hoje 20:30, 2 vagas" and moves on.
    val cardModifier = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) { }

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            colors = colors,
            elevation = elevation,
            modifier = cardModifier,
        ) {
            MatchCardContent(
                venueName = venueName,
                startsAtSeconds = startsAtSeconds,
                metaLabel = metaLabel,
                priceLabel = priceLabel,
                slotsLabel = slotsLabel,
                openSlots = openSlots,
                joinButtonLabel = joinButtonLabel,
                onJoinClick = onJoinClick,
            )
        }
    } else {
        Card(
            shape = shape,
            colors = colors,
            elevation = elevation,
            modifier = cardModifier,
        ) {
            MatchCardContent(
                venueName = venueName,
                startsAtSeconds = startsAtSeconds,
                metaLabel = metaLabel,
                priceLabel = priceLabel,
                slotsLabel = slotsLabel,
                openSlots = openSlots,
                joinButtonLabel = joinButtonLabel,
                onJoinClick = onJoinClick,
            )
        }
    }
}

@Composable
private fun MatchCardContent(
    venueName: String,
    startsAtSeconds: Long,
    metaLabel: String?,
    priceLabel: String?,
    slotsLabel: String?,
    openSlots: Int,
    joinButtonLabel: String?,
    onJoinClick: (() -> Unit)?,
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
                    text = venueName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatWhen(startsAtSeconds = startsAtSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metaLabel != null) {
                    Text(
                        text = metaLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (priceLabel != null) {
                    Text(
                        text = priceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (slotsLabel != null) {
                SlotBadge(label = slotsLabel, openSlots = openSlots)
            }
        }

        if (joinButtonLabel != null && onJoinClick != null) {
            CedarSecondaryButton(
                text = joinButtonLabel,
                onClick = onJoinClick,
                enabled = openSlots > 0,
            )
        }
    }
}
