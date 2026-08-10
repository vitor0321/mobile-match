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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Game
import kotlinx.collections.immutable.ImmutableList

/**
 * "TENHO 2 VAGAS" -> o aplicativo procura -> "QUEM QUER JOGAR?"
 *
 * Tela inicial do produto: lista as partidas com vagas em aberto na região do
 * jogador. TODO: trocar TopAppBar por MatchTopBar do cedarDS e as strings
 * hardcoded por Lyricist, seguindo o padrão de products/identity.
 */
internal class GameListStep : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val stepModel = koinScreenModel<GameListStepModel>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is GameListEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Vagas abertas") }) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            when {
                state.isLoading -> LoadingContent(Modifier.fillMaxSize().padding(padding))

                state.errorMessage != null -> MessageContent(
                    message = state.errorMessage.orEmpty(),
                    modifier = Modifier.fillMaxSize().padding(padding),
                )

                state.games.isEmpty() -> MessageContent(
                    message = "Nenhuma vaga aberta na sua região agora.",
                    modifier = Modifier.fillMaxSize().padding(padding),
                )

                else -> GameList(
                    games = state.games,
                    onJoin = { stepModel.onEvent(GameListEvents.JoinGame(it)) },
                    contentPadding = padding,
                )
            }
        }
    }
}

@Composable
private fun GameList(
    games: ImmutableList<Game>,
    onJoin: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = 12.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = games, key = { it.id }) { game ->
            GameCard(game = game, onJoin = { onJoin(game.id) })
        }
    }
}

@Composable
private fun GameCard(
    game: Game,
    onJoin: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = game.sport.label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${game.venueName} · ${game.neighborhood}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = game.startsAt,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${game.confirmedPlayers}/${game.totalPlayers} jogadores · " +
                    "${game.openSlots} ${if (game.openSlots == 1) "vaga" else "vagas"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            game.pricePerPlayer?.let { price ->
                Text(
                    text = "$price por jogador",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = onJoin) { Text("ENTRAR NO JOGO") }
            }
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

@Composable
private fun MessageContent(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}
