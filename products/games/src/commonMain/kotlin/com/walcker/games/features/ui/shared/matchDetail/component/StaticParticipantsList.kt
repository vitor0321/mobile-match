package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun StaticParticipantsList(
    participantIds: List<String>,
    organizerName: String,
    detail: MatchDetailStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        participantIds.forEachIndexed { index, _ ->
            Text(
                text = if (index == 0) organizerName else detail.anonymousPlayer(index + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
