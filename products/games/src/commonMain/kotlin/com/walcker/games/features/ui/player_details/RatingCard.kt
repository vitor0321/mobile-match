package com.walcker.games.features.ui.player_details

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
import com.walcker.games.features.domain.model.Rating
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatShortDate

/**
 * A single review: stars, optional comment, and the date it was left.
 *
 * The reviewer's name and photo are intentionally absent — resolving them needs one
 * profile read per review, which the ratings subcollection does not denormalize
 * yet. Adding it later is a data-layer change only.
 *
 * @param ratingLabel already-formatted score (e.g. `"4,0"`), supplied by the
 *   caller's strings layer so this composable stays locale-agnostic.
 */
@Composable
internal fun RatingCard(
    rating: Rating,
    ratingLabel: String,
    ratingAccessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.mdShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(
                    rating = rating.rating.toFloat(),
                    contentDescription = ratingAccessibilityLabel,
                )

                Text(
                    text = ratingLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (rating.comment.isNotBlank()) {
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val date = formatShortDate(rating.createdAtMs)
            if (date.isNotEmpty()) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
