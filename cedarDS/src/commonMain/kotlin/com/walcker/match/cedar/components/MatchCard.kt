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
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatWhen

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
    cityLabel: String? = null,
    joinButtonLabel: String? = null,
    onJoinClick: (() -> Unit)? = null,
    matchRating: Float? = null,
    matchRatingCountLabel: String? = null,
) {
    val shape = CedarTokens.radius.mdShape
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat)
    val cardModifier =
        modifier
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
                cityLabel = cityLabel,
                joinButtonLabel = joinButtonLabel,
                onJoinClick = onJoinClick,
                matchRating = matchRating,
                matchRatingCountLabel = matchRatingCountLabel,
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
                cityLabel = cityLabel,
                joinButtonLabel = joinButtonLabel,
                onJoinClick = onJoinClick,
                matchRating = matchRating,
                matchRatingCountLabel = matchRatingCountLabel,
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
    cityLabel: String?,
    joinButtonLabel: String?,
    onJoinClick: (() -> Unit)?,
    matchRating: Float?,
    matchRatingCountLabel: String?,
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
                if (matchRating != null && matchRatingCountLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                    ) {
                        RatingStars(rating = matchRating, starSize = 12.dp)
                        Text(
                            text = matchRatingCountLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                ) {
                    SlotBadge(label = slotsLabel, openSlots = openSlots)
                    if (cityLabel != null) {
                        Text(
                            text = cityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
