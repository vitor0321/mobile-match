package com.walcker.games.features.ui.gamelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.strings.GameListStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.collections.immutable.ImmutableList

/**
 * Home screen of the games product.
 *
 * Shows open matches with a sport filter and a radius slider.
 *
 * The title scrolls with the content instead of sitting in an app bar: the redesign
 * has no opaque bar on list screens, so the tinted canvas runs to the top edge and
 * the cards are what the eye lands on.
 */
internal class GameListStep : Screen {

    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<GameListStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val strings = rememberGamesStrings().strings.gameList

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is GameListEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        Scaffold(
            containerColor = CedarTokens.colors.canvas,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CedarScreenTitle(
                    title = strings.title,
                    subtitle = strings.subtitle,
                    modifier = Modifier.padding(
                        horizontal = CedarTokens.spacing.lg,
                        vertical = CedarTokens.spacing.md,
                    ),
                )

                if (state.preferencesLoaded) {
                    FilterBar(
                        strings = strings,
                        selectedSport = state.selectedSport,
                        radiusKm = state.radiusKm,
                        onSelectSport = { stepModel.onEvent(GameListEvents.SelectSport(it)) },
                        onRadiusChange = { stepModel.onEvent(GameListEvents.SetRadius(it)) },
                    )
                }

                when {
                    state.isLoading && state.games.isEmpty() ->
                        LoadingContent(Modifier.fillMaxSize())

                    state.errorMessage != null && state.games.isEmpty() ->
                        EmptyState(
                            message = state.errorMessage.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                        )

                    state.games.isEmpty() ->
                        EmptyState(
                            message = strings.emptyMessage,
                            modifier = Modifier.fillMaxSize(),
                        )

                    else -> GameList(
                        strings = strings,
                        games = state.games,
                        onJoin = { stepModel.onEvent(GameListEvents.JoinGame(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    strings: GameListStrings,
    selectedSport: Sport?,
    radiusKm: Double,
    onSelectSport: (Sport?) -> Unit,
    onRadiusChange: (Double) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = CedarTokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
            contentPadding = PaddingValues(horizontal = CedarTokens.spacing.lg),
        ) {
            item {
                SportChip(
                    label = strings.allSportsChip,
                    selected = selectedSport == null,
                    onClick = { onSelectSport(null) },
                )
            }
            items(items = Sport.entries) { sport ->
                SportChip(
                    label = sport.label,
                    selected = selectedSport == sport,
                    onClick = { onSelectSport(sport) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CedarTokens.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.radiusLabel(radiusKm.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = CedarTokens.spacing.sm),
            )
            Slider(
                value = radiusKm.toFloat(),
                onValueChange = { onRadiusChange(it.toDouble()) },
                valueRange = GameListState.MIN_RADIUS_KM.toFloat()..GameListState.MAX_RADIUS_KM.toFloat(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GameList(
    strings: GameListStrings,
    games: ImmutableList<Game>,
    onJoin: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
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
                // TODO(fase 2): trocar por onClick abrindo o detalhe da partida.
                joinButtonLabel = strings.joinButton,
                onJoinClick = { onJoin(game.id) },
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}
