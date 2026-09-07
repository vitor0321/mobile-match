package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun StaticParticipantsList(
    participantIds: List<String>,
    organizerName: String,
    organizerRating: Double,
    organizerRatingCount: Int,
    detail: MatchDetailStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        participantIds.forEachIndexed { index, _ ->
            if (index == 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = organizerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (organizerRatingCount > 0) {
                        RatingStars(rating = organizerRating.toFloat(), starSize = 12.dp)
                        Text(
                            text = detail.ratingsCount(organizerRatingCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    text = detail.anonymousPlayer(index + 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
