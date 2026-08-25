package com.walcker.games.features.ui.player_search

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
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens

private const val MAX_SPORTS_ON_CARD = 3

/**
 * A player in the search results: avatar, name, rating, favourite sports.
 *
 * The avatar was a grey circle with nothing in it when the player had no photo —
 * which is most of them. [PlayerAvatar] falls back to initials, so the row
 * identifies someone either way.
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
        onClick = { onPlayerSelected(player.userId) },
        shape = CedarTokens.radius.mdShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(
                displayName = player.displayName,
                photoUrl = player.photoUrl,
                size = PlayerAvatarSize.Medium,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                Text(
                    text = player.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                ) {
                    RatingStars(
                        rating = player.averageRating,
                        contentDescription = ratingAccessibilityLabel,
                    )
                    Text(
                        text = "$ratingLabel (${player.totalRatings})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Favourite sports. The match counter that used to sit here was
                // removed with the fields that never had a writer.
                if (player.favoriteSports.isNotEmpty()) {
                    Text(
                        text = player.favoriteSports
                            .take(MAX_SPORTS_ON_CARD)
                            .joinToString(separator = " · ") { it.label },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
