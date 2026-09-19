package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun PlayersPerTeamSlider(
    selectedCount: Int,
    strings: ManageMatchStrings,
    enabled: Boolean,
    onCountSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Text(
            text = strings.teamsPlayersPerTeamLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = strings.teamsPlayersPerTeamValue(selectedCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = selectedCount.toFloat(),
            onValueChange = { onCountSelected(it.toInt()) },
            valueRange = MIN_PLAYERS_PER_TEAM.toFloat()..MAX_PLAYERS_PER_TEAM.toFloat(),
            steps = MAX_PLAYERS_PER_TEAM - MIN_PLAYERS_PER_TEAM - 1,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
