package com.walcker.games.features.ui.playerProfile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.format.formatDecimal

@Composable
internal fun ProfileHeader(
    name: String?,
    email: String?,
    fallbackName: String,
    averageRating: Float,
    totalRatings: Int,
    ratingsCountLabel: (Int) -> String,
    ratingContentDescription: (Float) -> String,
    modifier: Modifier = Modifier,
) {
    val displayName = name ?: email ?: fallbackName
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        PlayerAvatar(displayName = displayName, size = PlayerAvatarSize.Large)
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (totalRatings > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(
                    rating = averageRating,
                    contentDescription = ratingContentDescription(averageRating),
                )
                Text(
                    text = "${formatDecimal(averageRating, 1)} (${ratingsCountLabel(totalRatings)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (email != null && email != displayName) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
