package com.walcker.games.features.ui.playerProfile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.shared.common.SportBadge
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun MySportsSection(
    availableSports: Set<Sport>,
    strings: PlayerProfileStrings,
    onSportToggled: (Sport) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderedSports = Sport.entries.sortedByDescending { it in availableSports }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        CedarSectionHeader(title = strings.sportsPreferenceTitle)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
        ) {
            items(orderedSports, key = { it.name }) { sport ->
                SportBadge(
                    sport = sport,
                    selected = sport in availableSports,
                    onClick = { onSportToggled(sport) },
                )
            }
        }
    }
}
