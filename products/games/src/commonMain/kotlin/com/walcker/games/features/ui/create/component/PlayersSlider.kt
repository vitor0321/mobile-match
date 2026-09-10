package com.walcker.games.features.ui.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.ui.create.CreateMatchState
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun PlayersSlider(
    totalPlayers: Int,
    strings: CreateMatchStrings,
    enabled: Boolean,
    onPlayersChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Text(
            text = strings.playersLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = strings.playersValue(totalPlayers),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = totalPlayers.toFloat(),
            onValueChange = { onPlayersChanged(it.toInt()) },
            valueRange =
                CreateMatchState.MIN_PLAYERS.toFloat()..CreateMatchState.MAX_PLAYERS.toFloat(),
            steps = CreateMatchState.MAX_PLAYERS - CreateMatchState.MIN_PLAYERS - 1,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
