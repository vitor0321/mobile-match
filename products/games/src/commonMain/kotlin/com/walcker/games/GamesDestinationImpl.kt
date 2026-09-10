package com.walcker.games

import cafe.adriel.voyager.core.screen.Screen
import com.walcker.games.features.ui.create.CreateMatchStep
import com.walcker.games.features.ui.home.GameListStep
import com.walcker.games.features.ui.myMatches.MyMatchesStep
import com.walcker.games.features.ui.playerProfile.PlayerProfileStep
import com.walcker.games.features.ui.search.SearchStep
import com.walcker.games.features.ui.shared.matchDetail.MatchDetailStep
import com.walcker.match.navigator.GamesDestination

internal class GamesDestinationImpl : GamesDestination {
    override fun gameList(): Screen = GameListStep()

    override fun search(): Screen = SearchStep()

    override fun create(): Screen = CreateMatchStep()

    override fun editMatch(matchId: String): Screen = CreateMatchStep(matchId)

    override fun myMatches(): Screen = MyMatchesStep()

    override fun playerProfile(): Screen = PlayerProfileStep()

    override fun matchDetail(matchId: String): Screen = MatchDetailStep(matchId)
}
