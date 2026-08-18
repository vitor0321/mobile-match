package com.walcker.games.features.ui.player_search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage as CoilAsyncImage
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.match.cedar.components.RatingStars

/**
 * Card component displaying a player search result.
 *
 * Shows: photo, name, rating, sports, match count.
 * Clickable to navigate to player details.
 */
@Composable
internal fun PlayerSearchResultCard(
    player: PlayerSearchResult,
    ratingLabel: String,
    ratingAccessibilityLabel: String,
    onPlayerSelected: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayerSelected(player.userId) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player photo
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                if (!player.photoUrl.isNullOrEmpty()) {
                    CoilAsyncImage(
                        model = player.photoUrl,
                        contentDescription = player.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }

            // Player info
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Name
                Text(
                    text = player.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )

                // Rating row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RatingStars(
                        rating = player.averageRating,
                        contentDescription = ratingAccessibilityLabel,
                    )
                    Text(
                        text = ratingLabel,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = "(${player.totalRatings})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Sports and match count
                Text(
                    text = buildString {
                        if (player.favoriteSports.isNotEmpty()) {
                            append(player.favoriteSports.take(2).joinToString(", ") { it.label })
                        }
                        if (player.matchesOrganized + player.matchesParticipated > 0) {
                            append(" • ")
                            append(
                                (player.matchesOrganized + player.matchesParticipated)
                                    .toString()
                            )
                            append(" partidas")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
