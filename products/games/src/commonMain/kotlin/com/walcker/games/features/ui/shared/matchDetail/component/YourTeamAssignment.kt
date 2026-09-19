package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun YourTeamAssignment(
    teamIndex: Int,
    detail: MatchDetailStrings,
    modifier: Modifier = Modifier,
) {
    val teamLabel = detail.teamLabel(teamIndex)
    Text(
        text = detail.yourTeamLabel(teamLabel),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CedarTokens.radius.mdShape)
                .padding(CedarTokens.spacing.md),
    )
}
