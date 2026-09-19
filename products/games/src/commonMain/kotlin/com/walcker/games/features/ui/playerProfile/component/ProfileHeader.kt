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

@Composable
internal fun ProfileHeader(
    name: String?,
    email: String?,
    fallbackName: String,
    averageRating: Float,
    totalRatings: Int,
    organizerAverageRating: Float,
    organizerTotalRatings: Int,
    asPlayerLabel: String,
    asOrganizerLabel: String,
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
            RatingSummaryRow(
                label = asPlayerLabel,
                rating = averageRating,
                contentDescription = "${ratingContentDescription(averageRating)} — ${ratingsCountLabel(totalRatings)}",
            )
        }
        if (organizerTotalRatings > 0) {
            RatingSummaryRow(
                label = asOrganizerLabel,
                rating = organizerAverageRating,
                contentDescription =
                    "${ratingContentDescription(organizerAverageRating)} — ${ratingsCountLabel(organizerTotalRatings)}",
            )
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

@Composable
private fun RatingSummaryRow(
    label: String,
    rating: Float,
    contentDescription: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RatingStars(
            rating = rating,
            contentDescription = contentDescription,
        )
    }
}
