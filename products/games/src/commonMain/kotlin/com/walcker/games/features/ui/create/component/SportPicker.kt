package com.walcker.games.features.ui.create.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun SportPicker(
    selected: Sport?,
    enabled: Boolean,
    onSelect: (Sport) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        Sport.entries.forEach { sport ->
            SportChip(
                label = sport.label,
                selected = selected == sport,
                onClick = { onSelect(sport) },
                enabled = enabled,
            )
        }
    }
}
