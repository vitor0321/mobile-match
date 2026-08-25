package com.walcker.games.features.ui.gamelist

import com.walcker.games.features.domain.model.Sport

internal sealed interface GameListEvents {
    data object Refresh : GameListEvents
    /** `null` clears the sport filter (shows all sports). */
    data class SelectSport(val sport: Sport?) : GameListEvents
    data class SetRadius(val radiusKm: Double) : GameListEvents
    /** Toque no card. Quem decide para onde ir é o model, não a UI. */
    data class SelectGame(val gameId: String) : GameListEvents
}

internal sealed interface GameListEffect {
    data class ShowMessage(val message: String) : GameListEffect
    data class NavigateToMatchDetail(val matchId: String) : GameListEffect
}
