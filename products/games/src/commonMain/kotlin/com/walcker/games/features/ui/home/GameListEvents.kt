package com.walcker.games.features.ui.home

import com.walcker.games.features.domain.shared.model.Sport

internal sealed interface GameListEvents {
    data object Refresh : GameListEvents

    data class SelectSport(
        val sport: Sport?,
    ) : GameListEvents

    data class SetRadius(
        val radiusKm: Double,
    ) : GameListEvents

    data class SelectGame(
        val gameId: String,
    ) : GameListEvents

    data object LoadMore : GameListEvents
}

internal sealed interface GameListEffect {
    data class ShowMessage(
        val message: String,
    ) : GameListEffect

    data class NavigateToMatchDetail(
        val matchId: String,
    ) : GameListEffect
}
