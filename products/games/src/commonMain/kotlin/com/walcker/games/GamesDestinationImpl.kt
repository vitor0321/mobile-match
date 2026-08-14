package com.walcker.games

import cafe.adriel.voyager.core.screen.Screen
import com.walcker.games.features.ui.gamelist.GameListStep
import com.walcker.games.features.ui.search.SearchStep
import com.walcker.match.navigator.GamesDestination

internal class GamesDestinationImpl : GamesDestination {
    override fun gameList(): Screen = GameListStep()
    override fun search(): Screen = SearchStep()
}
