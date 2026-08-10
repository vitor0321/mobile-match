package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Sport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fonte temporária, em memória, para o app rodar antes do backend existir.
 * Substituir por Firestore/API quando o match sair do papel.
 */
internal class InMemoryGameSource : GameSource {

    private val games = MutableStateFlow(SAMPLE_GAMES)

    override suspend fun openGames(): List<Game> = games.value

    override suspend fun joinGame(gameId: String) {
        games.update { current ->
            current.map { game ->
                if (game.id == gameId && game.hasOpenSlots) {
                    game.copy(confirmedPlayers = game.confirmedPlayers + 1)
                } else {
                    game
                }
            }
        }
    }

    private companion object {
        val SAMPLE_GAMES = listOf(
            Game(
                id = "1",
                sport = Sport.FUTSAL,
                venueName = "Green Ball",
                neighborhood = "União dos Cegos",
                startsAt = "Hoje, 20h",
                confirmedPlayers = 12,
                totalPlayers = 14,
                pricePerPlayer = "R$ 20",
                organizerName = "Vitor",
            ),
            Game(
                id = "2",
                sport = Sport.SOCIETY,
                venueName = "Arena Central",
                neighborhood = "União dos Cegos",
                startsAt = "Amanhã, 19h30",
                confirmedPlayers = 10,
                totalPlayers = 12,
                pricePerPlayer = "R$ 25",
                organizerName = "Rafael",
            ),
            Game(
                id = "3",
                sport = Sport.VOLEI,
                venueName = "Quadra do Parque",
                neighborhood = "Centro",
                startsAt = "Quinta, 21h",
                confirmedPlayers = 10,
                totalPlayers = 12,
                pricePerPlayer = null,
                organizerName = "Camila",
            ),
        )
    }
}
