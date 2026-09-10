package com.walcker.games.features.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.shared.common.AllSportsBadge
import com.walcker.games.features.ui.shared.common.SportBadge
import com.walcker.games.strings.GameListStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun SportChipsRow(
    strings: GameListStrings,
    selectedSport: Sport?,
    mySports: Set<Sport>,
    onSelectSport: (Sport?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
        contentPadding = PaddingValues(horizontal = CedarTokens.spacing.lg),
    ) {
        item {
            AllSportsBadge(
                label = strings.allSportsChip,
                selected = selectedSport == null,
                onClick = { onSelectSport(null) },
            )
        }
        val orderedSports = Sport.entries.sortedByDescending { it in mySports }
        items(items = orderedSports, key = { it.name }) { sport ->
            SportBadge(
                sport = sport,
                selected = selectedSport == sport,
                onClick = { onSelectSport(sport) },
            )
        }
    }
}
