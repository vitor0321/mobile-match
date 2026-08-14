package com.walcker.games.features.ui.gamelist

import com.walcker.games.features.domain.model.Sport

internal sealed interface GameListEvents {
    data object Refresh : GameListEvents
    data class JoinGame(val gameId: String) : GameListEvents
    /** `null` clears the sport filter (shows all sports). */
    data class SelectSport(val sport: Sport?) : GameListEvents
    data class SetRadius(val radiusKm: Double) : GameListEvents
}

internal sealed interface GameListEffect {
    data class ShowMessage(val message: String) : GameListEffect
}
