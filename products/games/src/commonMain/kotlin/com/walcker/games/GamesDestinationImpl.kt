package com.walcker.games

import cafe.adriel.voyager.core.screen.Screen
import com.walcker.games.features.ui.creatematch.CreateMatchStep
import com.walcker.games.features.ui.gamelist.GameListStep
import com.walcker.games.features.ui.mymatches.MyMatchesStep
import com.walcker.games.features.ui.playerprofile.PlayerProfileStep
import com.walcker.games.features.ui.search.SearchStep
import com.walcker.match.navigator.GamesDestination

internal class GamesDestinationImpl : GamesDestination {
    override fun gameList(): Screen = GameListStep()
    override fun search(): Screen = SearchStep()
    override fun create(): Screen = CreateMatchStep()
    override fun myMatches(): Screen = MyMatchesStep()
    override fun playerProfile(): Screen = PlayerProfileStep()
}
