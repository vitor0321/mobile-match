package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.match.cedar.tokens.CedarTokens

private const val BADGE_TINT = 0.15f

@Composable
internal fun StatusBadge(
    status: MatchStatus,
    isMatchOver: Boolean,
    detail: MatchDetailStrings,
    modifier: Modifier = Modifier,
) {
    val (label, color) =
        when {
            status == MatchStatus.CANCELLED ->
                detail.statusCancelled to MaterialTheme.colorScheme.error
            status == MatchStatus.FINISHED || isMatchOver ->
                detail.statusFinished to MaterialTheme.colorScheme.onSurfaceVariant
            status == MatchStatus.FULL ->
                detail.statusFull to MaterialTheme.colorScheme.error
            else ->
                detail.statusOpen to MaterialTheme.colorScheme.primary
        }

    Box(
        modifier =
            modifier
                .background(color.copy(alpha = BADGE_TINT), shape = CedarTokens.radius.pill)
                .padding(
                    horizontal = CedarTokens.spacing.sm,
                    vertical = CedarTokens.spacing.xxs,
                ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
