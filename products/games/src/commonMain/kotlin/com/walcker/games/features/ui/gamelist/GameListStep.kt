package com.walcker.games.features.ui.gamelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.strings.GameListStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.components.SportChip
import kotlinx.collections.immutable.ImmutableList

/**
 * Home screen of the games product.
 *
 * Shows open matches with sport chips filter, radius slider, and
 * pull-to-refresh. Delegates strings to Lyricist (ETAPA9).
 */
internal class GameListStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
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
            topBar = {
                CedarTopBar(
                    title = strings.title,
                    subtitle = strings.subtitle,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SportChip(
                    label = strings.allSportsChip,
                    selected = selectedSport == null,
                    onClick = { onSelectSport(null) },
                )
            }
            items(items = Sport.values().toList()) { sport ->
                SportChip(
                    label = sport.label,
                    selected = selectedSport == sport,
                    onClick = { onSelectSport(sport) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.radiusLabel(radiusKm.toInt()),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = 12.dp),
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
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = games, key = { it.id }) { game ->
            val playersLabel = strings.playersAndSlots(
                game.confirmedPlayers,
                game.totalPlayers,
                game.openSlots,
                if (game.openSlots == 1) "vaga" else "vagas",
            )
            MatchCard(
                sportLabel = game.sport.label,
                venueName = game.venueName,
                neighborhood = game.neighborhood,
                startsAtSeconds = game.startsAtSeconds,
                confirmedPlayers = game.confirmedPlayers,
                totalPlayers = game.totalPlayers,
                openSlots = game.openSlots,
                pricePerPlayer = game.pricePerPlayer,
                joinButtonLabel = strings.joinButton,
                playersAndSlotsLabel = playersLabel,
                perPlayerLabel = game.pricePerPlayer?.let { strings.perPlayer(it) },
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
