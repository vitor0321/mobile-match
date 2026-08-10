package com.walcker.games

import cafe.adriel.voyager.core.screen.Screen
import com.walcker.games.features.ui.gamelist.GameListStep
import com.walcker.match.navigator.GamesDestination

internal class GamesDestinationImpl : GamesDestination {
    override fun gameList(): Screen = GameListStep()
}
