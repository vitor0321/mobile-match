package com.walcker.games.features.ui.gamelist

internal sealed interface GameListEvents {
    data object Refresh : GameListEvents
    data class JoinGame(val gameId: String) : GameListEvents
}

internal sealed interface GameListEffect {
    data class ShowMessage(val message: String) : GameListEffect
}
