package com.walcker.games.features.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.strings.GameListStrings
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun GameList(
    strings: GameListStrings,
    games: ImmutableList<Game>,
    onClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.xs,
            ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        items(items = games, key = { it.id }) { game ->
            MatchCard(
                venueName = game.venueName,
                startsAtSeconds = game.startsAtSeconds,
                metaLabel = "${game.sport.label} · ${game.neighborhood}",
                priceLabel = game.pricePerPlayer?.let { strings.perPlayer(it) },
                slotsLabel = strings.slotsBadge(game.openSlots),
                openSlots = game.openSlots,
                onClick = { onClick(game.id) },
                matchRating = game.matchRating.toFloat().takeIf { game.matchRatingCount > 0 },
                matchRatingCountLabel =
                    strings.ratingsCount(game.matchRatingCount).takeIf { game.matchRatingCount > 0 },
            )
        }
    }
}
