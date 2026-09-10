package com.walcker.games.features.ui.home.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.ui.home.map.model.NearbyMatch
import com.walcker.games.strings.MapStrings
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.geo.formatDistance

@Composable
internal fun NearbyMatchesSheet(
    strings: MapStrings,
    matches: List<NearbyMatch>,
    onMatchTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CedarSectionHeader(
            title = strings.nearbyTitle,
            subtitle = strings.nearbySubtitle(matches.size),
            modifier =
                Modifier.padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.sm,
                ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding =
                PaddingValues(
                    start = CedarTokens.spacing.lg,
                    end = CedarTokens.spacing.lg,
                    top = CedarTokens.spacing.xs,
                    bottom = CedarTokens.spacing.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            items(items = matches, key = { it.game.id }) { nearby ->
                MatchCard(
                    venueName = nearby.game.venueName,
                    startsAtSeconds = nearby.game.startsAtSeconds,
                    metaLabel = "${nearby.game.sport.label} · ${formatDistance(nearby.distanceKm)}",
                    onClick = { onMatchTap(nearby.game.id) },
                )
            }
        }
    }
}
