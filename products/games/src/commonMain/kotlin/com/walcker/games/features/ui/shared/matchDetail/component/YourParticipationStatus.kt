package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun YourParticipationStatus(
    isConfirmed: Boolean,
    waitlistPosition: Int?,
    detail: MatchDetailStrings,
    modifier: Modifier = Modifier,
) {
    val container = if (isConfirmed) CedarTokens.colors.availableContainer else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (isConfirmed) CedarTokens.colors.availableText else MaterialTheme.colorScheme.onSurfaceVariant
    val title =
        if (isConfirmed) {
            detail.yourStatusConfirmedTitle
        } else {
            detail.yourStatusWaitlistTitle(waitlistPosition ?: 0)
        }
    val explanation = if (isConfirmed) detail.yourStatusConfirmedExplanation else detail.yourStatusWaitlistExplanation

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = container, shape = CedarTokens.radius.mdShape)
                .padding(CedarTokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = onContainer,
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            color = onContainer,
        )
    }
}
