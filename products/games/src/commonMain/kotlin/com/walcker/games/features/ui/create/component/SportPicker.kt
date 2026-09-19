package com.walcker.games.features.ui.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.shared.common.SportBadge
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun SportPicker(
    selected: Sport?,
    enabled: Boolean,
    mySports: Set<Sport>,
    onSelect: (Sport) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderedSports = Sport.entries.sortedByDescending { it in mySports }
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
        contentPadding = PaddingValues(horizontal = CedarTokens.spacing.lg),
    ) {
        items(orderedSports, key = { it.name }) { sport ->
            SportBadge(
                sport = sport,
                selected = selected == sport,
                onClick = { onSelect(sport) },
                enabled = enabled,
            )
        }
    }
}
