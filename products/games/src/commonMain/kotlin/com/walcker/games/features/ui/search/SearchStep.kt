package com.walcker.games.features.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.strings.LocalGamesStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.games.strings.GamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.MatchCard

/**
 * Search screen — filters the local cache by venue, neighborhood,
 * city or sport label. Search runs entirely client-side; the cache
 * is updated whenever the home screen refreshes.
 */
internal class SearchStep : Screen {

    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<SearchStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val gamesStrings = rememberGamesStrings().strings
        val strings = gamesStrings.search
        val cardStrings = gamesStrings.gameList

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is SearchEffect.ShowMessage ->
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
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { stepModel.onEvent(SearchEvents.QueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(strings.placeholder) },
                    singleLine = true,
                )

                if (state.results.isEmpty() && state.query.isNotBlank()) {
                    EmptyState(
                        message = strings.emptyForQuery(state.query),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.results, key = { it.id }) { game ->
                            val playersLabel = cardStrings.playersAndSlots(
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
                                joinButtonLabel = cardStrings.joinButton,
                                playersAndSlotsLabel = playersLabel,
                                perPlayerLabel = game.pricePerPlayer?.let { cardStrings.perPlayer(it) },
                                onJoinClick = { stepModel.onEvent(SearchEvents.JoinGame(game.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
