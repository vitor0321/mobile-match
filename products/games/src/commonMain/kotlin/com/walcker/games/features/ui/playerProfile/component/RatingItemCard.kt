package com.walcker.games.features.ui.playerProfile.component

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
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatShortDate

@Composable
internal fun RatingItemCard(
    rating: Rating,
    strings: PlayerProfileStrings,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.mdShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(CedarTokens.spacing.md)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(
                    rating = rating.rating.toFloat(),
                    contentDescription = strings.ratingContentDescription(rating.rating.toFloat()),
                )
                Text(
                    text = formatShortDate(epochMillis = rating.createdAtMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rating.comment.isNotEmpty()) {
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
