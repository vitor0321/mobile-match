package com.walcker.games.features.ui.shared.playerRatings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.strings.PlayerRatingsStrings
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun SortRow(
    selected: RatingSort,
    strings: PlayerRatingsStrings,
    onSortSelected: (RatingSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        items(RatingSort.entries) { sort ->
            SportChip(
                label = strings.labelFor(sort),
                selected = sort == selected,
                onClick = { onSortSelected(sort) },
            )
        }
    }
}

private fun PlayerRatingsStrings.labelFor(sort: RatingSort): String =
    when (sort) {
        RatingSort.RECENT -> sortRecent
        RatingSort.HIGHEST -> sortHighest
        RatingSort.LOWEST -> sortLowest
    }
