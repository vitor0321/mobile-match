package com.walcker.games.features.ui.search

internal sealed interface SearchEvents {
    data class QueryChanged(val query: String) : SearchEvents
    data class JoinGame(val gameId: String) : SearchEvents
}

internal sealed interface SearchEffect {
    data class ShowMessage(val message: String) : SearchEffect
}
