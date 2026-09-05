package com.walcker.games.features.ui.shared.playerDetails.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.PlayerDetails
import com.walcker.games.strings.PlayerDetailsStrings
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatShortDate

private val HeaderStarSize = 20.dp

@Composable
internal fun PlayerHeader(
    player: PlayerDetails,
    strings: PlayerDetailsStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        PlayerAvatar(
            displayName = player.displayName,
            photoUrl = player.photoUrl,
            size = PlayerAvatarSize.Large,
        )

        Text(
            text = player.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        RatingStars(
            rating = player.averageRating,
            starSize = HeaderStarSize,
            contentDescription = strings.ratingAccessibility(player.averageRating),
        )
        Text(
            text = strings.ratingValue(player.averageRating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = strings.ratingsCount(player.totalRatings),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        player.favoriteSports
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " · ") { it.label }
            ?.let { sports ->
                Text(
                    text = sports,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

        player.locationLabel()?.let { location ->
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val memberSince = formatShortDate(player.memberSinceMs)
        if (memberSince.isNotEmpty()) {
            Text(
                text = strings.memberSince(memberSince),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun PlayerDetails.locationLabel(): String? =
    listOfNotNull(
        neighborhood?.takeIf { it.isNotBlank() },
        city?.takeIf { it.isNotBlank() },
    ).takeIf { it.isNotEmpty() }?.joinToString(separator = " · ")
